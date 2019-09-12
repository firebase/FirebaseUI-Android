package com.firebase.uidemo;

import com.squareup.leakcanary.LeakCanary;

import androidx.multidex.MultiDexApplication;

public class FirebaseUIDemo extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}
