package com.mayank.socialfinder;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class Config extends Application {

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public Config(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.PREF, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // dark theme
    public int getDarkMode() {
        return sharedPreferences.getInt(Constants.PREF_SETTINGS_DARK_THEME, 0);
    }

    public void setDarkMode(int darkModeValue) {
        editor.putInt(Constants.PREF_SETTINGS_DARK_THEME, darkModeValue).apply();
    }

    // init screen
    public boolean getInitState() {
        return !sharedPreferences.getBoolean(Constants.PREF_SETTING_INIT, true);
    }

    public void setInitState(boolean isInitModeEnabled) {
        editor.putBoolean(Constants.PREF_SETTING_INIT, isInitModeEnabled).apply();
    }
}
