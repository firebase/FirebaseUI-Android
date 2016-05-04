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

package com.firebase.ui.auth.ui.idp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.choreographer.idp.provider.IDPResponse;
import com.firebase.ui.auth.ui.BaseActivity;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

public class IDPSignInContainerActivity extends IDPBaseActivity implements IDPProvider.IDPCallback {

    private static final String PROVIDER = "sign_in_provider";
    private static final String EMAIL = "email";
    private IDPProvider mIDPProvider;
    private String mProvider;
    private String mEmail;

    public static Intent createIntent(Context context,
                                      String provider,
                                      String email,
                                      ArrayList<IDPProviderParcel> availableProviderParcel,
                                      String appName) {
        return new Intent()
                .setClass(context, IDPSignInContainerActivity.class)
                .putExtra(PROVIDER, provider)
                .putExtra(EMAIL, email)
                .putExtra(ControllerConstants.EXTRA_PROVIDERS, availableProviderParcel)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProvider = getIntent().getStringExtra(PROVIDER);
        mEmail = getIntent().getStringExtra(EMAIL);
        IDPProviderParcel providerParcel = null;
        ArrayList<IDPProviderParcel> parcels =
                getIntent().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);
        for ( IDPProviderParcel parcel :  parcels) {
            if (parcel.getProviderType().equalsIgnoreCase(mProvider)) {
                providerParcel = parcel;
                break;
            }
        }
        if (providerParcel == null) {
            finish(BaseActivity.RESULT_CANCELED, new Intent());
            return;
        }
        if (mProvider.equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)) {
            mIDPProvider = new FacebookProvider(this, providerParcel);
        } else if (mProvider.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)) {
            mIDPProvider = new GoogleProvider(this, providerParcel);
        }
        mIDPProvider.setAuthenticationCallback(this);
        mIDPProvider.startLogin(this, mEmail);
    }

    @Override
    public void onSuccess(IDPResponse response) {
        Intent data = new Intent();
        data.putExtra(ControllerConstants.EXTRA_IDP_RESPONSE, response);
        finish(RESULT_OK, data);
    }

    @Override
    public void onFailure(Bundle extra) {
        Intent data = new Intent();
        finish(LOGIN_CANCELLED, data);
    }

    @Override
    public void finish(int resultCode, Intent data) {
        data.putParcelableArrayListExtra(
                ControllerConstants.EXTRA_PROVIDERS,
                getIntent().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS));
        super.finish(resultCode, data);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mIDPProvider.onActivityResult(requestCode, resultCode, data);
    }
}
