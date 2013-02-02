package com.cm.wifiscanner;

import com.cm.wifiscanner.util.ThemeManager;

import android.app.Activity;
import android.os.Bundle;

public class SettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getTheme());
        super.onCreate(savedInstanceState);
    }
}
