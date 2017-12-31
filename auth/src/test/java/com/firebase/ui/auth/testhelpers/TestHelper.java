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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestHelper {
    private static final String APPLICATION_ID = "testAppId";
    private static final String API_KEY = "fakeKey";
    private static final String FIREBASE_APP_NAME = "firebaseAppName";

    public static FirebaseApp initializeApp(Context context) {
        AuthUI.setApplicationContext(context);
        try {
            return FirebaseApp.initializeApp(
                    context,
                    new FirebaseOptions.Builder()
                            .setApiKey(API_KEY)
                            .setApplicationId(APPLICATION_ID)
                            .build(),
                    FIREBASE_APP_NAME);
        } catch (IllegalStateException e) {
            return FirebaseApp.getInstance(FIREBASE_APP_NAME);
        }
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
                FIREBASE_APP_NAME,
                idpConfigs,
                AuthUI.getDefaultTheme(),
                AuthUI.NO_LOGO,
                null,
                null,
                true,
                true);
    }

    public static void verifySmartLockSave(String providerId, String email, String password) {
        verifySmartLockSave(providerId, email, password, null);
    }

    public static void verifySmartLockSave(String providerId, String email,
                                           String password, String phoneNumber) {

        ArgumentCaptor<FirebaseUser> userCaptor = ArgumentCaptor.forClass(FirebaseUser.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IdpResponse> idpResponseCaptor = ArgumentCaptor.forClass(IdpResponse.class);

        verify(AuthHelperShadow.getSaveSmartLockInstance(null)).saveCredentialsOrFinish(
                userCaptor.capture(),
                passwordCaptor.capture(),
                idpResponseCaptor.capture());

        // Check email and password
        assertNotNull(userCaptor.getValue());
        assertEquals(email, userCaptor.getValue().getEmail());
        assertEquals(password, passwordCaptor.getValue());
        assertEquals(providerId, idpResponseCaptor.getValue().getProviderType());

        // Check phone number (if necessary)
        if (phoneNumber != null) {
            assertEquals(phoneNumber, userCaptor.getValue().getPhoneNumber());
        }

        // Check provider id
        if (providerId == null) {
            assertNull(idpResponseCaptor.getValue());
        } else {
            assertEquals(providerId, idpResponseCaptor.getValue().getProviderType());
        }
    }
}
