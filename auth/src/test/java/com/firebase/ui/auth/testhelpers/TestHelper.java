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

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public final class TestHelper {

    private static final String TAG = "TestHelper";
    private static final String DEFAULT_APP_NAME = "[DEFAULT]";
    private static final String MICROSOFT_PROVIDER = "microsoft.com";

    public static final FirebaseApp MOCK_APP;
    private static Context CONTEXT = ApplicationProvider.getApplicationContext();


    static {
        FirebaseApp app = mock(FirebaseApp.class);
        when(app.get(eq(FirebaseAuth.class))).thenReturn(mock(FirebaseAuth.class));
        when(app.getApplicationContext()).thenReturn(CONTEXT);
        when(app.getName()).thenReturn(DEFAULT_APP_NAME);
        MOCK_APP = app;
    }

    public static void initialize() {
        spyContextAndResources();
        AuthUI.setApplicationContext(CONTEXT);
        initializeApp(CONTEXT);
        initializeProviders();
    }

    private static void spyContextAndResources() {
        CONTEXT = spy(CONTEXT);
        when(CONTEXT.getApplicationContext())
                .thenReturn(CONTEXT);
        Resources spiedResources = spy(CONTEXT.getResources());
        when(CONTEXT.getResources()).thenReturn(spiedResources);
    }

    private static void initializeApp(Context context) {
        if (!FirebaseApp.getApps(context).isEmpty()) {
            return;
        }

        FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApiKey("fake")
                .setApplicationId("fake")
                .build());
    }

    private static void initializeProviders() {
        when(CONTEXT.getString(R.string.firebase_web_host)).thenReturn("abc");
        when(CONTEXT.getString(R.string.default_web_client_id)).thenReturn("abc");
        when(CONTEXT.getString(R.string.facebook_application_id)).thenReturn("abc");
    }

    public static FirebaseUser getMockFirebaseUser() {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn(TestConstants.UID);
        when(user.getEmail()).thenReturn(TestConstants.EMAIL);
        when(user.getDisplayName()).thenReturn(TestConstants.NAME);
        when(user.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);

        return user;
    }

    public static HelperActivityBase getHelperActivity(FlowParameters parameters) {
        AuthUI authUI = AuthUI.getInstance(parameters.appName);

        HelperActivityBase activity = mock(HelperActivityBase.class);
        when(activity.getFlowParams()).thenReturn(parameters);
        when(activity.getAuthUI()).thenReturn(authUI);
        when(activity.getAuth()).thenReturn(authUI.getAuth());

        return activity;
    }

    public static FlowParameters getFlowParameters(Collection<String> providerIds) {
        return getFlowParameters(providerIds, false);
    }

    public static FlowParameters getFlowParameters(Collection<String> providerIds,
                                                   boolean enableAnonymousUpgrade) {
        return getFlowParameters(providerIds, enableAnonymousUpgrade, null, false);
    }

    public static FlowParameters getFlowParameters(Collection<String> providerIds,
                                                   boolean enableAnonymousUpgrade,
                                                   AuthMethodPickerLayout customLayout,
                                                   boolean hasDefaultEmail) {
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
                case EMAIL_LINK_PROVIDER:
                    idpConfigs.add(new IdpConfig.EmailBuilder().enableEmailLinkSignIn()
                            .setActionCodeSettings(ActionCodeSettings.newBuilder().setUrl("URL")
                                    .setHandleCodeInApp(true).build()).build());
                    break;
                case EmailAuthProvider.PROVIDER_ID:
                    if (hasDefaultEmail) { idpConfigs.add(new IdpConfig.EmailBuilder()
                                .setDefaultEmail(TestConstants.EMAIL)
                                .build());
                    } else
                    {
                        idpConfigs.add(new IdpConfig.EmailBuilder().build());
                    }
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.PhoneBuilder().build());
                    break;
                case AuthUI.ANONYMOUS_PROVIDER:
                    idpConfigs.add(new IdpConfig.AnonymousBuilder().build());
                    break;
                case MICROSOFT_PROVIDER:
                    idpConfigs.add(new IdpConfig.MicrosoftBuilder().build());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
            }
        }
        return new FlowParameters(
                DEFAULT_APP_NAME,
                idpConfigs,
                null,
                AuthUI.getDefaultTheme(),
                AuthUI.NO_LOGO,
                null,
                null,
                true,
                true,
                enableAnonymousUpgrade,
                false,
                true,
                null,
                null,
                customLayout);
    }

    /**
     * Set a private, obfuscated field of an object.
     *
     * @param obj        the object to modify.
     * @param objClass   the object's class.
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
