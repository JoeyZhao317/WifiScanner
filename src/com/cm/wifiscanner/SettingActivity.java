package com.cm.wifiscanner;

import com.actionbarsherlock.app.SherlockActivity;
import com.cm.wifiscanner.util.ThemeManager;

import android.os.Bundle;

public class SettingActivity extends SherlockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getTheme());
        super.onCreate(savedInstanceState);
    }
}
