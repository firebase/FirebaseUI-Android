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

package com.firebase.ui.auth.testhelpers;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public final class TestHelper {

    private static final String TAG = "TestHelper";
    private static final String DEFAULT_APP_NAME = "[DEFAULT]";

    public static final FirebaseApp MOCK_APP;

    static {
        FirebaseApp app = mock(FirebaseApp.class);
        when(app.get(eq(FirebaseAuth.class))).thenReturn(mock(FirebaseAuth.class));
        when(app.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(app.getName()).thenReturn(DEFAULT_APP_NAME);
        MOCK_APP = app;
    }

    public static void initialize() {
        spyContextAndResources();
        AuthUI.setApplicationContext(RuntimeEnvironment.application);
        initializeApp(RuntimeEnvironment.application);
        initializeProviders();
    }

    private static void spyContextAndResources() {
        RuntimeEnvironment.application = spy(RuntimeEnvironment.application);
        when(RuntimeEnvironment.application.getApplicationContext())
                .thenReturn(RuntimeEnvironment.application);
        Resources spiedResources = spy(RuntimeEnvironment.application.getResources());
        when(RuntimeEnvironment.application.getResources()).thenReturn(spiedResources);
    }

    private static void initializeApp(Context context) {
        if (!FirebaseApp.getApps(context).isEmpty()) return;

        FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApiKey("fake")
                .setApplicationId("fake")
                .build());
    }

    private static void initializeProviders() {
        Context context = RuntimeEnvironment.application;
        when(context.getString(R.string.firebase_web_host)).thenReturn("abc");
        when(context.getString(R.string.default_web_client_id)).thenReturn("abc");
        when(context.getString(R.string.facebook_application_id)).thenReturn("abc");
        when(context.getString(R.string.twitter_consumer_key)).thenReturn("abc");
        when(context.getString(R.string.twitter_consumer_secret)).thenReturn("abc");
        when(context.getString(R.string.github_client_id)).thenReturn("abc");
        when(context.getString(R.string.github_client_secret)).thenReturn("abc");
    }

    public static FirebaseUser getMockFirebaseUser() {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn(TestConstants.UID);
        when(user.getEmail()).thenReturn(TestConstants.EMAIL);
        when(user.getDisplayName()).thenReturn(TestConstants.NAME);
        when(user.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);

        return user;
    }

    public static FlowParameters getFlowParameters(Collection<String> providerIds) {
        return getFlowParameters(providerIds, false);
    }

    public static FlowParameters getFlowParameters(Collection<String> providerIds,
                                                   boolean enableAnonymousUpgrade) {
        List<IdpConfig> idpConfigs = new ArrayList<>();
        for (String providerId : providerIds) {
            switch (providerId) {
                case GoogleAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.GoogleBuilder().build());
                    break;
                case FacebookAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.FacebookBuilder().build());
                    break;
                case TwitterAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.TwitterBuilder().build());
                    break;
                case GithubAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.GitHubBuilder().build());
                    break;
                case EmailAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.EmailBuilder().build());
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.PhoneBuilder().build());
                    break;
                case AuthUI.ANONYMOUS_PROVIDER:
                    idpConfigs.add(new IdpConfig.AnonymousBuilder().build());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
            }
        }
        return new FlowParameters(
                DEFAULT_APP_NAME,
                idpConfigs,
                AuthUI.getDefaultTheme(),
                AuthUI.NO_LOGO,
                null,
                null,
                true,
                true,
                enableAnonymousUpgrade,
                false);
    }

    /**
     * Set a private, obfuscated field of an object.
     * @param obj the object to modify.
     * @param objClass the object's class.
     * @param fieldClass the class of the target field.
     * @param fieldValue the value to use for the field.
     */
    public static <T, F> void setPrivateField(
            T obj,
            Class<T> objClass,
            Class<F> fieldClass,
            F fieldValue) {

        Field targetField = null;
        Field[] classFields = objClass.getDeclaredFields();
        for (Field field : classFields) {
            if (field.getType().equals(fieldClass)) {
                if (targetField != null) {
                    throw new IllegalStateException("Class " + objClass + " has multiple fields of type " + fieldClass);
                }

                targetField = field;
            }
        }

        if (targetField == null) {
            throw new IllegalStateException("Class " + objClass + " has no fields of type " + fieldClass);
        }

        targetField.setAccessible(true);
        try {
            targetField.set(obj, fieldValue);
        } catch (IllegalAccessException e) {
            Log.w(TAG, "Error setting field", e);
        }
    }
}
