package com.firebase.ui.auth.api;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

public class FactoryHeadlessAPI {

    private static HeadlessAPIWrapper sDefaultImpl;

    public static HeadlessAPIWrapper getHeadlessAPIWrapperInstance(String appName) {
        FirebaseApp firebaseApp = FirebaseApp.getInstance(appName);
        return new HeadlessAPIWrapperImpl(FirebaseAuth.getInstance(firebaseApp));
    }

    public static HeadlessAPIWrapper getHeadlessAPIWrapperInstance(FirebaseApp firebaseApp) {
        return new HeadlessAPIWrapperImpl(FirebaseAuth.getInstance(firebaseApp));
    }

    public static HeadlessAPIWrapper createHeadlessAPIWrapperInstance(
            Context context, String appName, JSONObject mGoogleServiceJSON) {
        // TODO(zhaojiac): change this to use Google Services plugin instead
        String apiaryKey =
                mGoogleServiceJSON
                        .optJSONArray("client")
                        .optJSONObject(1)
                        .optJSONArray("api_key")
                        .optJSONObject(0)
                        .optString("current_key");
        String applicationId =
                mGoogleServiceJSON
                        .optJSONArray("client")
                        .optJSONObject(1)
                        .optJSONArray("oauth_client")
                        .optJSONObject(0)
                        .optString("client_id");
        FirebaseOptions options
                = new FirebaseOptions.Builder()
                .setApiKey(apiaryKey)
                .setApplicationId(applicationId)
                .build();
        FirebaseApp curApp = FirebaseApp.initializeApp(context, options, appName);
        return new HeadlessAPIWrapperImpl(FirebaseAuth.getInstance(curApp));
    }
}
