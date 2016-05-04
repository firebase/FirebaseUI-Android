/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.api;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

public class FirebaseAuthWrapperFactory {

    private static FirebaseAuthWrapper sDefaultImpl;

    public static FirebaseAuthWrapper getFirebaseAuthWrapper(String appName) {
        FirebaseApp firebaseApp = FirebaseApp.getInstance(appName);
        return new FirebaseAuthWrapperImpl(FirebaseAuth.getInstance(firebaseApp));
    }

    public static FirebaseAuthWrapper getFirebaseAuthWrapper(FirebaseApp firebaseApp) {
        return new FirebaseAuthWrapperImpl(FirebaseAuth.getInstance(firebaseApp));
    }

    public static FirebaseAuthWrapper getFirebaseAuthWrapper(
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
        return new FirebaseAuthWrapperImpl(FirebaseAuth.getInstance(curApp));
    }
}
