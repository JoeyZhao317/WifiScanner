package com.cm.wifiscanner;

import com.cm.wifiscanner.hub.LoginUtils;
import com.cm.wifiscanner.util.Logger;
import com.cm.wifiscanner.wifi.AccessPoint;
import com.cm.wifiscanner.wifi.ProgressCategory;
import com.cm.wifiscanner.wifi.WifiDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.Status;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WifiScannerActivity extends PreferenceActivity implements DialogInterface.OnClickListener {

    private static final String TAG = "WifiScannerActivity";

    private static final String EXTRA_HUB_NAME = "user_name";
    private static final String EXTRA_HUB_PASSWORD = "user_password";
    private static final String TEST_URL = "www.baidu.com";

    private static final int INVALID_NETWORK_ID = -1;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private Handler mHandler;

    private WifiManager mWifiManager;
    private ProgressCategory mAccessPoints;
    private AccessPoint mSelectedAccessPoint;

    private DetailedState mLastState;
    private WifiInfo mLastInfo;
    private int mLastPriority;

    private boolean mResetNetworks = false;
    private int mKeyStoreNetworkId = -1;

    private WifiDialog mDialog;

    private Scanner mScanner;

    private String mLastHubUser;
    private String mLastHubPwd;

    public WifiScannerActivity() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(intent);
            }
        };

        mScanner = new Scanner();

        HandlerThread hanlderThread = new HandlerThread("hub_login_handler");
        hanlderThread.start();
        mHandler = new Handler(hanlderThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    Logger.debug(TAG, "received hub login message");

                    String name = bundle.getString(EXTRA_HUB_NAME);
                    String pwd = bundle.getString(EXTRA_HUB_PASSWORD);

                    if (LoginUtils.getInstance(WifiScannerActivity.this).loginHub(name, pwd, TEST_URL)) {
                        Toast.makeText(WifiScannerActivity.this, R.string.hub_login_success, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(WifiScannerActivity.this, R.string.hub_login_failure, Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        addPreferencesFromResource(R.xml.wifi_access_points);

        mAccessPoints = (ProgressCategory) findPreference("access_points");
        mAccessPoints.setOrderingAsAdded(false);

        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mReceiver, mFilter);
        if (mKeyStoreNetworkId != INVALID_NETWORK_ID) {
            connect(mKeyStoreNetworkId);
        }
        mKeyStoreNetworkId = INVALID_NETWORK_ID;
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
        mScanner.pause();

        if (mResetNetworks) {
            enableNetworks();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Looper looper = mHandler.getLooper();
        if (looper != null) {
            looper.quit();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            mSelectedAccessPoint = (AccessPoint) preference;
            showDialog(mSelectedAccessPoint, false);
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(action)) {
            if (mSelectedAccessPoint != null && mSelectedAccessPoint.networkId != INVALID_NETWORK_ID) {
                mSelectedAccessPoint = null;
            }
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            updateConnectionState(WifiInfo.getDetailedStateOf((SupplicantState)
                    intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            updateConnectionState(((NetworkInfo) intent.getParcelableExtra(
            WifiManager.EXTRA_NETWORK_INFO)).getDetailedState());
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);

            if (!TextUtils.isEmpty(mLastHubUser)) {
                Message msg = mHandler.obtainMessage(0);
                Bundle data = new Bundle();
                data.putString(EXTRA_HUB_NAME, mLastHubUser);
                data.putString(EXTRA_HUB_PASSWORD, mLastHubPwd);
                msg.setData(data);
                mHandler.sendMessage(msg);

                mLastHubUser = null;
                mLastHubPwd = null;
            }
        }

        System.out.println("handleEvent(): " + action);
    }

    private void enableNetworks() {
        for (int i = mAccessPoints.getPreferenceCount() - 1; i >= 0; --i) {
            WifiConfiguration config = ((AccessPoint) mAccessPoints.getPreference(i)).getConfig();
            if (config != null && config.status != Status.ENABLED) {
                mWifiManager.enableNetwork(config.networkId, false);
            }
        }

        mResetNetworks = false;
    }

    private void saveNetworks() {
        // Always save the configuration with all networks enabled.
        enableNetworks();
        mWifiManager.saveConfiguration();
        updateAccessPoints();
    }

    private void updateAccessPoints() {
        List<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        if (configs != null) {
            mLastPriority = 0;
            for (WifiConfiguration config : configs) {
                if (config.priority > mLastPriority) {
                    mLastPriority = config.priority;
                }

                // Shift the status to make enableNetworks() more efficient.
                if (config.status == Status.CURRENT) {
                    config.status = Status.ENABLED;
                } else if (mResetNetworks && config.status == Status.DISABLED) {
                    config.status = Status.CURRENT;
                }

                AccessPoint accessPoint = new AccessPoint(this, config);
                accessPoint.update(mLastInfo, mLastState);
                accessPoints.add(accessPoint);
            }
        }

        List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0
                        || result.capabilities.contains("[IBSS]")) {
                    continue;
                }

                boolean found = false;
                for (AccessPoint accessPoint : accessPoints) {
                    if (accessPoint.update(result)) {
                        found = true;
                    }
                }
                if (!found) {
                    accessPoints.add(new AccessPoint(this, result));
                }
            }
        }

        mAccessPoints.removeAll();
        for (AccessPoint accessPoint : accessPoints) {
            mAccessPoints.addPreference(accessPoint);
        }
    }

    private void updateConnectionState(DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (state == DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }

        for (int i = mAccessPoints.getPreferenceCount() - 1; i >= 0; --i) {
            ((AccessPoint) mAccessPoints.getPreference(i)).update(mLastInfo, mLastState);
        }

        if (mResetNetworks && (state == DetailedState.CONNECTED ||
                state == DetailedState.DISCONNECTED || state == DetailedState.FAILED)) {
            updateAccessPoints();
            enableNetworks();
        }
    }

    private void updateWifiState(int state) {
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            mScanner.resume();
            updateAccessPoints();
        } else {
            mScanner.pause();
            mAccessPoints.removeAll();
        }
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new WifiDialog(this, this, accessPoint, edit);
        mDialog.show();
    }

    private void forget(int networkId) {
        mWifiManager.removeNetwork(networkId);
        saveNetworks();
    }

    private void connect(int networkId) {
        if (networkId == INVALID_NETWORK_ID) {
            return;
        }

        // Reset the priority of each network if it goes too high.
        if (mLastPriority > 1000000) {
            for (int i = mAccessPoints.getPreferenceCount() - 1; i >= 0; --i) {
                AccessPoint accessPoint = (AccessPoint) mAccessPoints.getPreference(i);
                if (accessPoint.networkId != INVALID_NETWORK_ID) {
                    WifiConfiguration config = new WifiConfiguration();
                    config.networkId = accessPoint.networkId;
                    config.priority = 0;
                    mWifiManager.updateNetwork(config);
                }
            }
            mLastPriority = 0;
        }

        // Set to the highest priority and save the configuration.
        WifiConfiguration config = new WifiConfiguration();
        config.networkId = networkId;
        config.priority = ++mLastPriority;
        mWifiManager.updateNetwork(config);
        saveNetworks();

        // Connect to network by disabling others.
        mWifiManager.enableNetwork(networkId, true);
        mWifiManager.reconnect();
        mResetNetworks = true;
    }

    private boolean requireKeyStore(WifiConfiguration config) {
//        if (WifiDialog.requireKeyStore(config) &&
//                KeyStore.getInstance().test() != KeyStore.NO_ERROR) {
//            mKeyStoreNetworkId = config.networkId;
//            Credentials.getInstance().unlock(this);
//            return true;
//        }
        return false;
    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                return;
            }

            sendEmptyMessageDelayed(0, 6000);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            forget(mSelectedAccessPoint.networkId);
        } else if (which == WifiDialog.BUTTON_SUBMIT && mDialog != null) {
            WifiConfiguration config = mDialog.getConfig();

            if (config == null) {
                if (mSelectedAccessPoint != null && !requireKeyStore(mSelectedAccessPoint.getConfig())) {
                    connect(mSelectedAccessPoint.networkId);
                    mLastHubUser = mSelectedAccessPoint.mHubUser;
                    mLastHubPwd = mSelectedAccessPoint.mHubPassword;
                }
            } else if (config.networkId != INVALID_NETWORK_ID) {
                if (mSelectedAccessPoint != null) {
                    mWifiManager.updateNetwork(config);
                    saveNetworks();
                }
            } else {
                int networkId = mWifiManager.addNetwork(config);
                if (networkId != INVALID_NETWORK_ID) {
                    mWifiManager.enableNetwork(networkId, false);
                    config.networkId = networkId;
                    if (mDialog.edit || requireKeyStore(config)) {
                        saveNetworks();
                    } else {
                        connect(networkId);
                        mLastHubUser = mSelectedAccessPoint.mHubUser;
                        mLastHubPwd = mSelectedAccessPoint.mHubPassword;
                    }
                }
            }
        }
    }
}
