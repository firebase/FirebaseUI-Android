package com.firebase.firebaseui_android;

import com.firebase.client.Firebase;

public class DemoApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}