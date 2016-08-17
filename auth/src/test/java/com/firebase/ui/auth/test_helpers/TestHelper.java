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

package com.firebase.ui.auth.test_helpers;

import android.content.Context;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.ProviderHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHelper {
    private static final String APPLICATION_ID = "testAppId";
    private static final String API_KEY = "fakeKey";
    private static final String FIREBASE_APP_NAME = "firebaseAppName";

    public static FirebaseApp initializeApp(Context context) {
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

    public static FlowParameters getFlowParameters(Context context, List<String> providerIds) {
        return new FlowParameters(
                FIREBASE_APP_NAME,
                ProviderHelper.getProviderParcels(context, providerIds),
                AuthUI.getDefaultTheme(),
                AuthUI.NO_LOGO,
                null,
                true);
    }

    public static FirebaseUser makeMockFirebaseUser() {
        FirebaseUser mockFirebaseUser = mock(FirebaseUser.class);
        when(mockFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
        when(mockFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);
        return mockFirebaseUser;
    }

    public static GoogleApiAvailability makeMockGoogleApiAvailability() {
        GoogleApiAvailability availability = mock(GoogleApiAvailability.class);
        when(availability.isGooglePlayServicesAvailable(any(Context.class)))
                .thenReturn(ConnectionResult.SUCCESS);

        return availability;
    }
}
