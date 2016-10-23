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

package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.firebase.ui.auth.ui.AcquireEmailHelper;
import com.firebase.ui.auth.ui.email.SignInNoPasswordActivity;
import com.google.android.gms.auth.api.credentials.Credential;

public class EmailHintContainer {
    private static final int RC_HINT = 13;
    private AcquireEmailHelper mAcquireEmailHelper;
    private BaseHelper mHelper;

    public EmailHintContainer(BaseHelper helper) {
        mHelper = helper;
    }

    public void onActivityResult(Fragment fragment, int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_HINT && data != null) {
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            if (credential == null) {
                // If the hint picker is cancelled show the SignInNoPasswordActivity
                fragment.startActivityForResult(
                        SignInNoPasswordActivity.createIntent(
                                fragment.getContext(),
                                mHelper.getFlowParams(),
                                null),
                        AcquireEmailHelper.RC_SIGN_IN);
                return;
            }
            mAcquireEmailHelper.checkAccountExists(credential.getId());
        } else {
            mAcquireEmailHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_HINT && data != null) {
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            if (credential == null) {
                // If the hint picker is cancelled show the SignInNoPasswordActivity
                activity.startActivityForResult(
                        SignInNoPasswordActivity.createIntent(
                                activity,
                                mHelper.getFlowParams(),
                                null),
                        AcquireEmailHelper.RC_SIGN_IN);
                return;
            }
            mAcquireEmailHelper.checkAccountExists(credential.getId());
        } else {
            mAcquireEmailHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    public EmailHintContainer trySignInWithEmailAndPassword(Fragment fragment) {
        mAcquireEmailHelper = new AcquireEmailHelper(mHelper);
        FirebaseAuthWrapper apiWrapper =
                FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mHelper.getAppName());

        PendingIntent hintIntent = apiWrapper.getEmailHintIntent(fragment.getActivity());
        if (hintIntent != null) {
            try {
                fragment.startIntentSenderForResult(hintIntent.getIntentSender(),
                                                    RC_HINT,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    null);
                return this;
            } catch (IntentSender.SendIntentException e) {
                Log.e("EmailHintContainer", "Failed to send Credentials intent.", e);
            }
        }
        mHelper.finish(Activity.RESULT_CANCELED, new Intent());
        return null;
    }

    public EmailHintContainer trySignInWithEmailAndPassword(FragmentActivity activity) {
        mAcquireEmailHelper = new AcquireEmailHelper(mHelper);
        FirebaseAuthWrapper apiWrapper =
                FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mHelper.getAppName());

        PendingIntent hintIntent = apiWrapper.getEmailHintIntent(activity);
        if (hintIntent != null) {
            try {
                activity.startIntentSenderForResult(hintIntent.getIntentSender(),
                                                    RC_HINT,
                                                    null,
                                                    0,
                                                    0,
                                                    0,
                                                    null);
                return this;
            } catch (IntentSender.SendIntentException e) {
                Log.e("EmailHintContainer", "Failed to send Credentials intent.", e);
            }
        }
        mHelper.finish(Activity.RESULT_CANCELED, new Intent());
        return null;
    }
}
