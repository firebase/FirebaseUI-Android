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

package com.firebase.ui.auth.util.signincontainer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.provider.AuthCredentialHelper;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IdpProvider;
import com.firebase.ui.auth.provider.IdpProvider.IdpCallback;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.FragmentHelper;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.User;
import com.firebase.ui.auth.ui.idp.CredentialSignInHandler;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IdpSignInContainer extends FragmentBase implements IdpCallback {
    private static final String TAG = "IDPSignInContainer";
    private static final int RC_WELCOME_BACK_IDP = 4;

    private IdpProvider mIdpProvider;
    @Nullable
    private SaveSmartLock mSaveSmartLock;

    public static void signIn(FragmentActivity activity, FlowParameters parameters, User user) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (!(fragment instanceof IdpSignInContainer)) {
            IdpSignInContainer result = new IdpSignInContainer();

            Bundle bundle = FragmentHelper.getFlowParamsBundle(parameters);
            bundle.putParcelable(ExtraConstants.EXTRA_USER, user);
            result.setArguments(bundle);

            try {
                fm.beginTransaction().add(result, TAG).disallowAddToBackStack().commit();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Cannot add fragment", e);
            }
        }
    }

    public static IdpSignInContainer getInstance(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment instanceof IdpSignInContainer) {
            return (IdpSignInContainer) fragment;
        } else {
            return null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSaveSmartLock = mHelper.getSaveSmartLockInstance(getActivity());

        User user = User.getUser(getArguments());
        String provider = user.getProvider();

        AuthUI.IdpConfig providerConfig = null;
        for (AuthUI.IdpConfig config : mHelper.getFlowParams().providerInfo) {
            if (config.getProviderId().equalsIgnoreCase(provider)) {
                providerConfig = config;
                break;
            }
        }

        if (providerConfig == null) {
            // we don't have a provider to handle this
            finish(ResultCodes.CANCELED, IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
            return;
        }

        if (provider.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new GoogleProvider(
                    getActivity(),
                    providerConfig,
                    user.getEmail());
        } else if (provider.equalsIgnoreCase(FacebookAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new FacebookProvider(
                    getContext(), providerConfig, mHelper.getFlowParams().themeId);
        } else if (provider.equalsIgnoreCase(TwitterAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new TwitterProvider(getContext());
        }

        mIdpProvider.setAuthenticationCallback(this);
        mIdpProvider.startLogin(getActivity());
    }

    @Override
    public void onSuccess(final IdpResponse response) {
        AuthCredential credential = AuthCredentialHelper.getAuthCredential(response);
        mHelper.getFirebaseAuth()
                .signInWithCredential(credential)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Failure authenticating with credential"))
                .addOnCompleteListener(new CredentialSignInHandler(
                        getActivity(),
                        mHelper,
                        mSaveSmartLock,
                        RC_WELCOME_BACK_IDP,
                        response));
    }

    @Override
    public void onFailure(Bundle extra) {
        finish(ResultCodes.CANCELED, IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_WELCOME_BACK_IDP) {
            finish(resultCode, data);
        } else {
            mIdpProvider.onActivityResult(requestCode, resultCode, data);
        }
    }
}
