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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.credentials.CredentialSaveActivity;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import org.junit.Assert;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestHelper {
    private static final String APPLICATION_ID = "testAppId";
    private static final String API_KEY = "fakeKey";

    public static void initialize() {
        spyContextAndResources();
        AuthUI.setApplicationContext(RuntimeEnvironment.application);
        FirebaseApp app = initializeApp(RuntimeEnvironment.application);
        injectMockFirebaseAuth(app);
        initializeProviders();
    }

    private static void spyContextAndResources() {
        RuntimeEnvironment.application = spy(RuntimeEnvironment.application);
        when(RuntimeEnvironment.application.getApplicationContext())
                .thenReturn(RuntimeEnvironment.application);
        Resources spiedResources = spy(RuntimeEnvironment.application.getResources());
        when(RuntimeEnvironment.application.getResources()).thenReturn(spiedResources);
    }

    private static FirebaseApp initializeApp(Context context) {
        try {
            return FirebaseApp.initializeApp(
                    context,
                    new FirebaseOptions.Builder()
                            .setApiKey(API_KEY)
                            .setApplicationId(APPLICATION_ID)
                            .build(),
                    FirebaseApp.DEFAULT_APP_NAME);
        } catch (IllegalStateException e) {
            return FirebaseApp.getInstance(FirebaseApp.DEFAULT_APP_NAME);
        }
    }

    /**
     * This method finds the map of FirebaseAuth instances and injects of a mock instance associated
     * with the given FirebaseApp for testing purposes.
     */
    private static void injectMockFirebaseAuth(FirebaseApp app) {
        for (Field field : FirebaseAuth.class.getDeclaredFields()) {
            field.setAccessible(true);

            Object o;
            try {
                o = field.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (NullPointerException e) {
                continue;
            }

            Type genericType = field.getGenericType();
            if (o instanceof Map && genericType instanceof ParameterizedType) {
                Type[] parameterTypes = ((ParameterizedType) genericType).getActualTypeArguments();
                if (parameterTypes.length != 2 || parameterTypes[0] != String.class
                        || parameterTypes[1] != FirebaseAuth.class) {
                    continue;
                }

                //noinspection unchecked
                Map<String, FirebaseAuth> instances = (Map<String, FirebaseAuth>) o;

                FirebaseAuth.getInstance(app);
                for (String id : instances.keySet()) {
                    instances.put(id, mock(FirebaseAuth.class));
                }

                break;
            }
        }

        when(FirebaseAuth.getInstance(app).setFirebaseUIVersion(anyString()))
                .thenReturn(Tasks.<Void>forResult(null));
    }

    private static void initializeProviders() {
        Context context = RuntimeEnvironment.application;
        when(context.getString(R.string.default_web_client_id)).thenReturn("abc");
        when(context.getString(R.string.facebook_application_id)).thenReturn("abc");
        when(context.getString(R.string.twitter_consumer_key)).thenReturn("abc");
        when(context.getString(R.string.twitter_consumer_secret)).thenReturn("abc");
    }

    public static FirebaseUser getMockFirebaseUser() {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn(TestConstants.UID);
        when(user.getEmail()).thenReturn(TestConstants.EMAIL);
        when(user.getDisplayName()).thenReturn(TestConstants.NAME);
        when(user.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);

        return user;
    }

    public static FlowParameters getFlowParameters(List<String> providerIds) {
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
                case EmailAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.EmailBuilder().build());
                    break;
                case PhoneAuthProvider.PROVIDER_ID:
                    idpConfigs.add(new IdpConfig.PhoneBuilder().build());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
            }
        }
        return new FlowParameters(
                FirebaseApp.DEFAULT_APP_NAME,
                idpConfigs,
                AuthUI.getDefaultTheme(),
                AuthUI.NO_LOGO,
                null,
                null,
                true,
                true);
    }

    public static void verifyCredentialSaveStarted(@NonNull  Activity activity,
                                                   @Nullable String providerId,
                                                   @Nullable String email,
                                                   @Nullable String password,
                                                   @Nullable String phoneNumber) {

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        // Verify that CredentialSaveActivity is next up
        Assert.assertEquals(startedIntent.getComponent().getClassName(),
                CredentialSaveActivity.class.getName());

        // Check the credential passed
        Credential credential = startedIntent.getParcelableExtra(ExtraConstants.EXTRA_CREDENTIAL);

        // Check the password
        assertEquals(credential.getPassword(), password);

        // Non-password credentials have a provider ID
        if (TextUtils.isEmpty(password)) {
            assertEquals(credential.getAccountType(),
                    ProviderUtils.providerIdToAccountType(providerId));
        }

        // ID can either be email or phone number
        if (!TextUtils.isEmpty(phoneNumber)) {
            assertEquals(credential.getId(), phoneNumber);
        } else {
            assertEquals(credential.getId(), email);
        }
    }
}
