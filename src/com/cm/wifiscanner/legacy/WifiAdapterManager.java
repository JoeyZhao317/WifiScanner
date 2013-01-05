
package com.cm.wifiscanner.legacy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;

import java.util.List;

public class WifiAdapterManager {

    public static final int WIFI_STATE_ENABLING = WifiManager.WIFI_STATE_ENABLING;
    public static final int WIFI_STATE_ENABLED = WifiManager.WIFI_STATE_ENABLED;
    public static final int WIFI_STATE_DISABLING = WifiManager.WIFI_STATE_DISABLING;
    public static final int WIFI_STATE_DISABLED = WifiManager.WIFI_STATE_DISABLED;
    public static final int WIFI_STATE_UNKNOWN = WifiManager.WIFI_STATE_UNKNOWN;
    public static final int WIFI_RESULTS_AVAILABLE = 0xFF;

    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;

    public static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public interface WifiStateChangeListener {
        public void onWifiStateChanged(int state);
        public void onStateChanged(int state);
    }

    private static final String NOT_AVAILABLE = "N/A";
    private final WifiManager mWifiManager;
    private final WifiInfo mWifiInfo;
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> mWifiConfigurations;
    private WifiLock mWifiLock;

    private BroadcastReceiver mWifiStateReciever;
    private WifiStateChangeListener mListener;

    private Context mContext;

    public WifiAdapterManager(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();

        if (mWifiManager.startScan()) {
            mWifiConfigurations = mWifiManager.getConfiguredNetworks();

            mWifiStateReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mListener != null) {
                        String action = intent.getAction();
                        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                            int state = intent.getIntExtra(WifiManager.EXTRA_NEW_STATE, WIFI_STATE_UNKNOWN);
                            mListener.onWifiStateChanged(state);
                        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                            DetailedState state = WifiInfo.getDetailedStateOf((SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
                            mListener.onStateChanged(state.ordinal());
                        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                            DetailedState state = ((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
                            mListener.onStateChanged(state.ordinal());
                        } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                            mWifiList = mWifiManager.getScanResults();
                            mListener.onWifiStateChanged(WIFI_RESULTS_AVAILABLE);
                        }
                    }
                }
            };

            context.registerReceiver(mWifiStateReciever, new IntentFilter(
                    WifiManager.WIFI_STATE_CHANGED_ACTION));
            context.registerReceiver(mWifiStateReciever, new IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }

        mContext = context;
    }

    public void destroy() {
        if (mWifiStateReciever != null) {
            mContext.unregisterReceiver(mWifiStateReciever);
            mWifiStateReciever = null;
        }
    }

    public void toggleWifi() {
        final boolean enabled = mWifiManager.isWifiEnabled();
        mWifiManager.setWifiEnabled(!enabled);
    }

    public void connetionConfiguration(int index) {
        if (index < mWifiConfigurations.size()) {
            mWifiManager.enableNetwork(mWifiConfigurations.get(index).networkId, true);
        }
    }

    public int checkWifiState() {
        return mWifiManager.getWifiState();
    }

    public List<ScanResult> getScanResult() {
        return mWifiList;
    }

    public String getMacAddress() {
        return (mWifiInfo == null) ? NOT_AVAILABLE : mWifiInfo.getMacAddress();
    }

    public String getBSSID() {
        return (mWifiInfo == null) ? NOT_AVAILABLE : mWifiInfo.getBSSID();
    }

    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    public String getWifiInfo() {
        return (mWifiInfo == null) ? NOT_AVAILABLE : mWifiInfo.toString();
    }

    public void addNetwork(WifiConfiguration wcg) {
        int wcgID = mWifiManager.addNetwork(wcg);
        mWifiManager.enableNetwork(wcgID, true);
    }

    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

    private void ensureLock() {
        if (mWifiLock != null) {
            mWifiLock = mWifiManager.createWifiLock("lock");
        }
    }

    public void requireLock() {
        ensureLock();
        mWifiLock.acquire();
    }

    public void releaseLock() {
        ensureLock();
        mWifiLock.release();
    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    public void setWifiEnabled(boolean enabled) {
        mWifiManager.setWifiEnabled(enabled);

        if (!enabled && mWifiList != null) {
            mWifiList.clear();
        }
    }

    public void setWifiStateChangeListener(WifiStateChangeListener listener) {
        mListener = listener;
    }
}
