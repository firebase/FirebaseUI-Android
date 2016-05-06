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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.IDPController;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.choreographer.idp.provider.IDPResponse;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

public class AuthMethodPickerActivity
        extends IDPBaseActivity
        implements IDPProvider.IDPCallback, View.OnClickListener {

    private static final String TAG = "AuthMethodPicker";
    private ArrayList<IDPProviderParcel> mProviderParcels;
    private ArrayList<IDPProvider> mIDPProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nascar_layout);
        Button emailButton = (Button) findViewById(R.id.email_provider);
        emailButton.setOnClickListener(this);
    mProviderParcels = getIntent().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);
        populateIDPList(mProviderParcels);
    }

    public static Intent createIntent(
            Context context, String appName, ArrayList<IDPProviderParcel> parcels) {
    return new Intent()
        .setClass(context, AuthMethodPickerActivity.class)
        .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
        .putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, parcels)
        .putExtra(EXTRA_ID, IDPController.NASCAR_SCREEN);
    }

    private void populateIDPList(ArrayList<IDPProviderParcel> mProviders) {
        mIDPProviders = new ArrayList<>();
        for (IDPProviderParcel providerParcel : mProviders) {
            switch (providerParcel.getProviderType()) {
                case FacebookAuthProvider.PROVIDER_ID :
                    mIDPProviders.add(new FacebookProvider(this, providerParcel));
                    break;
                case GoogleAuthProvider.PROVIDER_ID:
                    mIDPProviders.add(new GoogleProvider(this, providerParcel));
                    break;
                case EmailAuthProvider.PROVIDER_ID:
                    findViewById(R.id.email_provider).setVisibility(View.VISIBLE);
                    break;
                default:
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Encountered unknown IDPProvider parcel with type: "
                                + providerParcel.getProviderType());
                    }
            }
        }
        LinearLayout btnHolder = (LinearLayout) findViewById(R.id.btn_holder);
        for(final IDPProvider provider: mIDPProviders) {
            View loginButton = null;
            switch (provider.getProviderId()) {
                case GoogleAuthProvider.PROVIDER_ID:
                    loginButton = getLayoutInflater()
                            .inflate(R.layout.idp_button_google, btnHolder, false);

                    break;
                case FacebookAuthProvider.PROVIDER_ID:
                    loginButton = getLayoutInflater()
                            .inflate(R.layout.idp_button_facebook, btnHolder, false);
                    break;
                default:
                    Log.e(TAG, "No button for provider " + provider.getProviderId());
            }
            if (loginButton != null) {
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        provider.startLogin(AuthMethodPickerActivity.this, null);
                    }
                });
                provider.setAuthenticationCallback(this);
                btnHolder.addView(loginButton, 0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for(IDPProvider provider : mIDPProviders) {
           provider.onActivityResult(requestCode, resultCode, data);
        }
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
    public void onClick(View v) {
        if (v.getId() == R.id.email_provider) {
           finish(IDPBaseActivity.EMAIL_LOGIN_NEEDED, new Intent());
        }
    }
}
