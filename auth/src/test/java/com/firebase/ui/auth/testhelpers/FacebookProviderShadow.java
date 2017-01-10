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

import com.facebook.login.LoginResult;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.google.firebase.auth.FacebookAuthProvider;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Implements(FacebookProvider.class)
public class FacebookProviderShadow {
    private static final String FAKE_ACCESS_TOKEN = "fake_access_token";
    private IdpResponse mMockIdpResponse;
    private IdpCallback mCallback;

    public FacebookProviderShadow() {
        if (mMockIdpResponse == null) {
            mMockIdpResponse = mock(IdpResponse.class);
            when(mMockIdpResponse.getProviderType()).thenReturn(FacebookAuthProvider.PROVIDER_ID);
            when(mMockIdpResponse.getIdpToken()).thenReturn(FAKE_ACCESS_TOKEN);
        }
    }

    @SuppressWarnings("checkstyle:methodname")
    public void __constructor__(Activity activity, List<String> scopes) {}

    @Implementation
    public void setAuthenticationCallback(IdpCallback idpCallback) {
        mCallback = idpCallback;
    }

    @Implementation
    public void onSuccess(final LoginResult loginResult) {
        mCallback.onSuccess(mMockIdpResponse);
    }
}
