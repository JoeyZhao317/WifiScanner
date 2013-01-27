package com.cm.wifiscanner.util;

public class ThemeManager {

    public static int THEME_DEFAULT = com.actionbarsherlock.R.style.Sherlock___Theme;
    public static int THEME_LIGHT = com.actionbarsherlock.R.style.Sherlock___Theme_Light;

    private static int THEME;

    public static void setLightTheme(int theme) {
        THEME = theme;
    }

    public static boolean isLightTheme() {
        return THEME == THEME_LIGHT;
    }

    public static int getTheme() {
        return THEME;
    }
}
