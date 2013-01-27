package com.cm.wifiscanner.jabber;

import com.actionbarsherlock.app.SherlockActivity;
import com.cm.wifiscanner.util.ThemeManager;

import android.os.Bundle;

public class JabberActivity extends SherlockActivity {

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getTheme());
        super.onCreate(savedInstanceState);
    }
}
