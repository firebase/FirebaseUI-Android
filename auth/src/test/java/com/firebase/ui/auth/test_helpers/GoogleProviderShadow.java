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

import android.app.Activity;
import android.os.Bundle;

import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IDPProvider;
import com.firebase.ui.auth.provider.IDPProviderParcel;
import com.firebase.ui.auth.provider.IDPResponse;
import com.google.firebase.auth.GoogleAuthProvider;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Implements(GoogleProvider.class)
public class GoogleProviderShadow {
    private static final String FAKE_TOKEN = "fake_token";
    private IDPResponse mMockIdpResponse;
    private Bundle mMockIdpResponseBundle;
    private IDPProvider.IDPCallback mCallback;

    public GoogleProviderShadow() {
        if(mMockIdpResponseBundle == null) {
            mMockIdpResponseBundle = mock(Bundle.class);
        }
        if (mMockIdpResponse == null) {
            mMockIdpResponse = mock(IDPResponse.class);
            when(mMockIdpResponse.getProviderType()).thenReturn(GoogleAuthProvider.PROVIDER_ID);
            when(mMockIdpResponse.getResponse()).thenReturn(mMockIdpResponseBundle);
            when(mMockIdpResponseBundle
                    .getString(GoogleProvider.TOKEN_KEY)).thenReturn(FAKE_TOKEN);
        }
    }

    public void __constructor__(Activity activity, IDPProviderParcel parcel, String email) {}


    @Implementation
    public void setAuthenticationCallback(IDPProvider.IDPCallback idpCallback) {
        mCallback = idpCallback;
    }

    @Implementation
    public void startLogin(Activity activity) {
        mCallback.onSuccess(mMockIdpResponse);
    }
}
