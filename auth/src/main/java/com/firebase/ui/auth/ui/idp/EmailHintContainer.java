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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.firebase.ui.auth.ui.AcquireEmailHelper;
import com.firebase.ui.auth.ui.BaseFragment;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.email.SignInNoPasswordActivity;
import com.firebase.ui.auth.util.BaseHelper;
import com.firebase.ui.auth.util.EmailFlowUtil;
import com.firebase.ui.auth.util.FirebaseAuthWrapper;
import com.firebase.ui.auth.util.FirebaseAuthWrapperFactory;
import com.google.android.gms.auth.api.credentials.Credential;

public class EmailHintContainer {
    private static final int RC_HINT = 13;
    private AcquireEmailHelper mAcquireEmailHelper;
    private BaseHelper mHelper;


    public EmailHintContainer(BaseHelper helper) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_HINT && data != null) {
            Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
            if (credential == null) {
                // If the hint picker is cancelled show the SignInNoPasswordActivity
                startActivityForResult(
                        SignInNoPasswordActivity.createIntent(
                                getContext(),
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

    public void trySignInWithEmailAndPassword() {
        mAcquireEmailHelper = new AcquireEmailHelper(mHelper);
        FirebaseAuthWrapper apiWrapper =
                FirebaseAuthWrapperFactory.getFirebaseAuthWrapper(mHelper.getAppName());

        PendingIntent hintIntent = apiWrapper.getEmailHintIntent(getActivity());
        if (hintIntent != null) {
            try {
                startIntentSenderForResult(hintIntent.getIntentSender(),
                                           RC_HINT,
                                           null,
                                           0,
                                           0,
                                           0,
                                           null);
                return;
            } catch (IntentSender.SendIntentException e) {
                Log.e("EmailHintContainer", "Failed to send Credentials intent.", e);
            }
        }
        mHelper.finish(Activity.RESULT_CANCELED, new Intent());
    }

    public static EmailHintContainer getInstance(FragmentActivity activity,
                                                 FlowParameters parameters,
                                                 String tag) {
        EmailHintContainer result;

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null || !(fragment instanceof EmailHintContainer)) {
            result = new EmailHintContainer();

            Bundle bundle = new Bundle();
            bundle.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, parameters);
            result.setArguments(bundle);

            ft.add(result, tag).disallowAddToBackStack().commit();
        } else {
            result = (EmailHintContainer) fragment;
        }

        return result;
    }
}
