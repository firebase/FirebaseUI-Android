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

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.CredentialsUtil;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SaveSmartLock extends SmartLockBase<Void> {
    private static final String TAG = "SaveSmartLock";
    private static final int RC_SAVE = 100;

    private CredentialsClient mCredentialsClient;

    private String mName;
    private String mEmail;
    private String mPassword;
    private String mProfilePictureUri;
    private IdpResponse mResponse;

    @Nullable
    public static SaveSmartLock getInstance(HelperActivityBase activity) {
        SaveSmartLock result;

        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (!(fragment instanceof SaveSmartLock)) {
            result = new SaveSmartLock();
            result.setArguments(activity.getFlowParams().toBundle());
            try {
                fm.beginTransaction().add(result, TAG).disallowAddToBackStack().commit();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Cannot add fragment", e);
                return null;
            }
        } else {
            result = (SaveSmartLock) fragment;
        }

        return result;
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            finish();
        } else if (task.getException() instanceof ResolvableApiException) {
            ResolvableApiException rae = (ResolvableApiException) task.getException();

            // Try to resolve the save request. This will prompt the user if
            // the credential is new.
            try {
                startIntentSenderForResult(rae.getResolution().getIntentSender(), RC_SAVE);
            } catch (IntentSender.SendIntentException e) {
                // Could not resolve the request
                Log.e(TAG, "STATUS: Failed to send resolution.", e);
                finish();
            }
        } else {
            Log.w(TAG, "Non-resolvable exception: " + task.getException());
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SAVE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "SAVE: Canceled by user");
            }
            finish();
        }
    }

    private void finish() {
        finish(Activity.RESULT_OK, mResponse.toIntent());
    }

    /**
     * If SmartLock is enabled and Google Play Services is available, save the credentials.
     * Otherwise, finish the calling Activity with {@link Activity#RESULT_OK}.
     * <p>
     * Note: saveCredentialsOrFinish cannot be called immediately after getInstance because onCreate
     * has not yet been called.
     *
     * @param firebaseUser Firebase user to save in Credential.
     * @param password     (optional) password for email credential.
     * @param response     (optional) an {@link IdpResponse} representing the result of signing in.
     */
    public void saveCredentialsOrFinish(FirebaseUser firebaseUser,
                                        @Nullable String password,
                                        @Nullable IdpResponse response) {
        mResponse = response;

        if (!getFlowParams().enableCredentials) {
            finish();
            return;
        }

        mName = firebaseUser.getDisplayName();
        mEmail = firebaseUser.getEmail();
        mPassword = password;
        mProfilePictureUri = firebaseUser.getPhotoUrl() != null
                ? firebaseUser.getPhotoUrl().toString()
                : null;

        if (getActivity() == null) {
            throw new IllegalStateException("Can't save credentials in null Activity");
        }

        // Build credentials client and kick off the save
        mCredentialsClient = Credentials.getClient(getActivity());
        Credential credential = CredentialsUtil.buildCredential(
                mEmail, mPassword, mName, mProfilePictureUri, mResponse);

        if (credential == null) {
            Log.e(TAG, "Unable to save null credential!");
            finish();
            return;
        }

        mCredentialsClient.save(credential)
                .addOnCompleteListener(this);
    }

}
