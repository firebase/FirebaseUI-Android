/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.ui.phone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.FirebaseAuthError;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * Activity to control the entire phone verification flow. Plays host to {@link
 * VerifyPhoneNumberFragment} and {@link SubmitConfirmationCodeFragment}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneVerificationActivity extends AppCompatBase {
    private enum VerificationState {
        VERIFICATION_NOT_STARTED, VERIFICATION_STARTED, VERIFIED
    }

    private static final String PHONE_VERIFICATION_LOG_TAG = "PhoneVerification";

    private static final long SHORT_DELAY_MILLIS = 750;
    @VisibleForTesting static final long AUTO_RETRIEVAL_TIMEOUT_MILLIS = 120000;

    private static final String KEY_VERIFICATION_PHONE = "KEY_VERIFICATION_PHONE";
    private static final String KEY_STATE = "KEY_STATE";

    private AlertDialog mAlertDialog;
    @VisibleForTesting CompletableProgressDialog mProgressDialog;
    private Handler mHandler;
    private String mPhoneNumber;
    private String mVerificationId;
    private Boolean mIsDestroyed = false;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;
    private VerificationState mVerificationState;

    public static Intent createIntent(Context context, FlowParameters flowParams, Bundle params) {
        return HelperActivityBase.createBaseIntent(
                context, PhoneVerificationActivity.class, flowParams)
                .putExtra(ExtraConstants.EXTRA_PARAMS, params);
    }

    @Override
    protected void onCreate(final Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.fui_activity_register_phone);

        mHandler = new Handler();
        mVerificationState = VerificationState.VERIFICATION_NOT_STARTED;
        if (savedInstance != null && !savedInstance.isEmpty()) {
            mPhoneNumber = savedInstance.getString(KEY_VERIFICATION_PHONE);

            if (savedInstance.getSerializable(KEY_STATE) != null) {
                mVerificationState = (VerificationState) savedInstance.getSerializable(KEY_STATE);
            }
            return;
        }

        Bundle params = getIntent().getExtras().getBundle(ExtraConstants.EXTRA_PARAMS);
        VerifyPhoneNumberFragment fragment = VerifyPhoneNumberFragment.newInstance
                (getFlowParams(), params);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_verify_phone, fragment, VerifyPhoneNumberFragment.TAG)
                .disallowAddToBackStack()
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Activity can be restarted in any of the following states
        // 1) VERIFICATION_STARTED
        // 2) SMS_RETRIEVED
        // 3) INSTANT_VERIFIED
        // 4) VERIFIED
        // For the first three cases, we can simply resubscribe to the
        // OnVerificationStateChangedCallbacks
        // For 4, we simply finish the activity
        if (mVerificationState.equals(VerificationState.VERIFICATION_STARTED)) {
            sendCode(mPhoneNumber, false);
        } else if (mVerificationState == VerificationState.VERIFIED) {
            // activity was recreated when verified dialog was displayed
            finish(getAuthHelper().getCurrentUser());
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            mVerificationState = VerificationState.VERIFICATION_NOT_STARTED;
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_STATE, mVerificationState);
        outState.putString(KEY_VERIFICATION_PHONE, mPhoneNumber);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        mHandler.removeCallbacksAndMessages(null);
        dismissLoadingDialog();
        super.onDestroy();
    }

    void verifyPhoneNumber(String phoneNumber, boolean forceResend) {
        sendCode(phoneNumber, forceResend);
        if (forceResend) {
            showLoadingDialog(getString(R.string.fui_resending));
        } else {
            showLoadingDialog(getString(R.string.fui_verifying));
        }
    }

    public void submitConfirmationCode(@NonNull String confirmationCode) {
        if (TextUtils.isEmpty(mVerificationId) || TextUtils.isEmpty(confirmationCode)) {
            // This situation should never happen except in the case of an extreme race
            // condition, so we will just ignore the submission.
            // See: https://github.com/firebase/FirebaseUI-Android/issues/922
            Log.w(PHONE_VERIFICATION_LOG_TAG,
                    String.format("submitConfirmationCode: mVerificationId is %s ; " +
                            "confirmationCode is %s", TextUtils.isEmpty(mVerificationId) ? "empty" : "not empty",
                            TextUtils.isEmpty(confirmationCode) ? "empty" : "not empty"));
            return;
        }

        showLoadingDialog(getString(R.string.fui_verifying));
        signIn(PhoneAuthProvider.getCredential(mVerificationId, confirmationCode));
    }

    private void onVerificationSuccess(@NonNull final PhoneAuthCredential phoneAuthCredential) {
        if (TextUtils.isEmpty(phoneAuthCredential.getSmsCode())) {
            signIn(phoneAuthCredential);
        } else {
            //Show Fragment if it is not already visible
            showSubmitCodeFragment();
            SubmitConfirmationCodeFragment submitConfirmationCodeFragment =
                    getSubmitConfirmationCodeFragment();


            showLoadingDialog(getString(R.string.fui_retrieving_sms));
            if (submitConfirmationCodeFragment != null) {
                submitConfirmationCodeFragment.setConfirmationCode(String.valueOf
                        (phoneAuthCredential.getSmsCode()));
            }
            signIn(phoneAuthCredential);
        }
    }

    private void onCodeSent() {
        completeLoadingDialog(getString(R.string.fui_code_sent));
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismissLoadingDialog();
                showSubmitCodeFragment();
            }
        }, SHORT_DELAY_MILLIS);
    }

    private void onVerificationFailed(@NonNull FirebaseException ex) {
        dismissLoadingDialog();

        if (ex instanceof FirebaseAuthException) {
            FirebaseAuthError error = FirebaseAuthError.fromException((FirebaseAuthException) ex);

            switch (error) {
                case ERROR_INVALID_PHONE_NUMBER:
                    VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                            getSupportFragmentManager().findFragmentByTag(VerifyPhoneNumberFragment.TAG);

                    if (verifyPhoneNumberFragment != null) {
                        verifyPhoneNumberFragment.showError(
                                getString(R.string.fui_invalid_phone_number));
                    }
                    break;
                case ERROR_TOO_MANY_REQUESTS:
                    showAlertDialog(getString(R.string.fui_error_too_many_attempts), null);
                    break;
                case ERROR_QUOTA_EXCEEDED:
                    showAlertDialog(getString(R.string.fui_error_quota_exceeded), null);
                    break;
                default:
                    Log.w(PHONE_VERIFICATION_LOG_TAG, error.getDescription(), ex);
                    showAlertDialog(error.getDescription(), null);
            }
        } else {
            Log.w(PHONE_VERIFICATION_LOG_TAG, ex.getLocalizedMessage());
            showAlertDialog(ex.getLocalizedMessage(), null);
        }
    }

    private void sendCode(String phoneNumber, boolean forceResend) {
        mPhoneNumber = phoneNumber;
        mVerificationState = VerificationState.VERIFICATION_STARTED;

        getAuthHelper().getPhoneAuthProvider().verifyPhoneNumber(
                phoneNumber,
                AUTO_RETRIEVAL_TIMEOUT_MILLIS,
                TimeUnit.MILLISECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        if (!mIsDestroyed) {
                            PhoneVerificationActivity.this.onVerificationSuccess(phoneAuthCredential);
                        }
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException ex) {
                        if (!mIsDestroyed) {
                            PhoneVerificationActivity.this.onVerificationFailed(ex);
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        mVerificationId = verificationId;
                        mForceResendingToken = forceResendingToken;
                        if (!mIsDestroyed) {
                            PhoneVerificationActivity.this.onCodeSent();
                        }
                    }
                },
                forceResend ? mForceResendingToken : null);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    protected AlertDialog getAlertDialog() {
        // It is hard to test AlertDialogs currently with robo electric. See:
        // https://github.com/robolectric/robolectric/issues/1944
        // We just test that the error was not displayed inline
        return mAlertDialog;
    }

    private void showSubmitCodeFragment() {
        // idempotent function
        if (getSubmitConfirmationCodeFragment() == null) {
            SubmitConfirmationCodeFragment f = SubmitConfirmationCodeFragment.newInstance(
                    getFlowParams(), mPhoneNumber);
            FragmentTransaction t = getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_verify_phone, f, SubmitConfirmationCodeFragment.TAG)
                    .addToBackStack(null);

            if (!isFinishing() && !mIsDestroyed) {
                t.commitAllowingStateLoss();
            }
        }
    }

    private void finish(FirebaseUser user) {
        IdpResponse response = new IdpResponse.Builder(
                new User.Builder(PhoneAuthProvider.PROVIDER_ID, null)
                        .setPhoneNumber(user.getPhoneNumber())
                        .build())
                .build();
        finish(RESULT_OK, response.toIntent());
    }

    private void showAlertDialog(@NonNull String s, DialogInterface.OnClickListener
            onClickListener) {
        mAlertDialog = new AlertDialog.Builder(this)
                .setMessage(s)
                .setPositiveButton(R.string.fui_incorrect_code_dialog_positive_button_text, onClickListener)
                .show();
    }

    private void signIn(@NonNull PhoneAuthCredential credential) {
        getAuthHelper().getFirebaseAuth()
                .signInWithCredential(credential)
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(final AuthResult authResult) {
                        mVerificationState = VerificationState.VERIFIED;
                        completeLoadingDialog(getString(R.string.fui_verified));

                        // Activity can be recreated before this message is handled
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!mIsDestroyed) {
                                    dismissLoadingDialog();
                                    finish(authResult.getUser());
                                }
                            }
                        }, SHORT_DELAY_MILLIS);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dismissLoadingDialog();
                        //incorrect confirmation code
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            FirebaseAuthError error = FirebaseAuthError.fromException(
                                    (FirebaseAuthInvalidCredentialsException) e);

                            switch (error) {
                                case ERROR_INVALID_VERIFICATION_CODE:
                                    showAlertDialog(
                                            getString(R.string.fui_incorrect_code_dialog_body),
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    getSubmitConfirmationCodeFragment()
                                                            .setConfirmationCode("");
                                                }
                                            });
                                    break;
                                case ERROR_SESSION_EXPIRED:
                                    showAlertDialog(
                                            getString(R.string.fui_error_session_expired),
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    getSubmitConfirmationCodeFragment()
                                                            .setConfirmationCode("");
                                                }
                                            });
                                    break;
                                default:
                                    Log.w(PHONE_VERIFICATION_LOG_TAG, error.getDescription(), e);
                                    showAlertDialog(error.getDescription(), null);
                            }
                        } else {
                            showAlertDialog(e.getLocalizedMessage(), null);
                        }
                    }
                });
    }

    private void completeLoadingDialog(String content) {
        if (mProgressDialog != null) {
            mProgressDialog.onComplete(content);
        }
    }

    private void showLoadingDialog(String message) {
        dismissLoadingDialog();

        if (mProgressDialog == null) {
            mProgressDialog = CompletableProgressDialog.show(getSupportFragmentManager());
        }

        mProgressDialog.setMessage(message);
    }

    private void dismissLoadingDialog() {
        if (mProgressDialog != null && !isFinishing()) {
            mProgressDialog.dismissAllowingStateLoss();
            mProgressDialog = null;
        }
    }

    private SubmitConfirmationCodeFragment getSubmitConfirmationCodeFragment() {
        return (SubmitConfirmationCodeFragment) getSupportFragmentManager().findFragmentByTag
                (SubmitConfirmationCodeFragment.TAG);
    }
}
