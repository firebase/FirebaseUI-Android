package com.firebase.uidemo;

import com.firebase.client.Firebase;

public class DemoApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
