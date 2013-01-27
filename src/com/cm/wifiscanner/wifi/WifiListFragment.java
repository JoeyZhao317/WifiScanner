package com.cm.wifiscanner.wifi;

import com.actionbarsherlock.app.SherlockListFragment;
import com.cm.wifiscanner.legacy.WifiAdapterManager;

import android.os.Bundle;

public class WifiListFragment extends SherlockListFragment {

    private WifiAdapterManager mAdapterMgr = null;

    public WifiListFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapterMgr = WifiAdapterManager.getInstance(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
