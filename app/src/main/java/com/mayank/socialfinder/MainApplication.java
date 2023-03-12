package com.mayank.socialfinder;

import android.app.Application;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this, new DynamicColorsOptions.Builder().setThemeOverlay(R.style.Theme_SocialFinder_Monet).build());
    }

}