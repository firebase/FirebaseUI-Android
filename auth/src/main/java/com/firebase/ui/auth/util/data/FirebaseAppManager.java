package com.firebase.ui.auth.util.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;

import java.util.UUID;

public class FirebaseAppManager {

    private FirebaseApp mScratchApp;
    public synchronized FirebaseApp getFirebaseApp(Context context) {
        if (mScratchApp == null) {
            FirebaseApp app = FirebaseApp.getInstance();
            String randomName = UUID.randomUUID().toString();
            mScratchApp = FirebaseApp.initializeApp(context, app.getOptions(), randomName);
        }
        return mScratchApp;
    }
}
