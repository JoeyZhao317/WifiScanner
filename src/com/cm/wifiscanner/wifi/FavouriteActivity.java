package com.cm.wifiscanner.wifi;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Window;

import android.os.Bundle;

public class FavouriteActivity extends SherlockListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

}
