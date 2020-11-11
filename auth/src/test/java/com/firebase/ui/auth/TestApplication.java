package com.firebase.ui.auth;

import android.app.Application;

/**
 * Used when Robolectric testing UI components which depend on the theme.s
 */
public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.FirebaseUI);
    }

}
