package com.firebase.uidemo;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

public class FirebaseUIDemo extends MultiDexApplication {
    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
    }
}
