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

package com.firebase.ui.auth.ui.phone;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.FirebaseAuthError;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.testhelpers.AuthHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.PHONE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.PHONE_NO_COUNTRY_CODE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.YE_COUNTRY_CODE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.YE_RAW_PHONE;
import static com.firebase.ui.auth.ui.phone.PhoneVerificationActivity.AUTO_RETRIEVAL_TIMEOUT_MILLIS;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PhoneVerificationActivityTest {
    private PhoneVerificationActivity mActivity;
    private TextView mErrorEditText;
    private Button mSendCodeButton;
    private EditText mPhoneEditText;
    private CountryListSpinner mCountryListSpinner;

    @Captor
    ArgumentCaptor<PhoneAuthProvider.OnVerificationStateChangedCallbacks> callbacksArgumentCaptor;
    @Mock
    PhoneAuthProvider.ForceResendingToken forceResendingToken;
    @Mock
    PhoneAuthCredential credential;
    @Mock
    FirebaseUser mockFirebaseUser;

    private String verificationId = "hjksdf737hc";

    private PhoneVerificationActivity createActivity() {
        Intent startIntent = PhoneVerificationActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        Collections.singletonList(AuthUI.PHONE_VERIFICATION_PROVIDER)), null);
        return Robolectric.buildActivity(PhoneVerificationActivity.class).withIntent(startIntent)
                .create(new Bundle()).start().visible().get();
    }

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
        initMocks(this);
        mActivity = createActivity();
        mPhoneEditText = (EditText) mActivity.findViewById(R.id.phone_number);
        mErrorEditText = (TextView) mActivity.findViewById(R.id.phone_number_error);
        mSendCodeButton = (Button) mActivity.findViewById(R.id.send_code);
        mCountryListSpinner = (CountryListSpinner) mActivity.findViewById(R.id.country_list);
    }

    @Test
    public void testPhoneNumberFromSmartlock_prePopulatesPhoneNumberInBundle() {
        Intent startIntent = PhoneVerificationActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        Collections.singletonList(AuthUI.PHONE_VERIFICATION_PROVIDER)),
                YE_RAW_PHONE);

        mActivity = Robolectric.buildActivity(PhoneVerificationActivity.class).withIntent
                (startIntent).create(new Bundle()).start().visible().get();

        VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                mActivity.getSupportFragmentManager()
                        .findFragmentByTag(VerifyPhoneNumberFragment.TAG);
        assertNotNull(verifyPhoneNumberFragment);
        mPhoneEditText = (EditText) mActivity.findViewById(R.id.phone_number);
        mCountryListSpinner = (CountryListSpinner) mActivity.findViewById(R.id.country_list);

        assertEquals(PHONE_NO_COUNTRY_CODE, mPhoneEditText.getText().toString());
        assertEquals(YE_COUNTRY_CODE,
                     String.valueOf(((CountryInfo) mCountryListSpinner.getTag()).countryCode));
    }

    @Test
    public void testBadPhoneNumber_showsInlineError() {
        VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                mActivity.getSupportFragmentManager()
                        .findFragmentByTag(VerifyPhoneNumberFragment.TAG);
        assertNotNull(verifyPhoneNumberFragment);

        mSendCodeButton.performClick();
        assertEquals(mErrorEditText.getText(), mActivity.getString(R.string.fui_invalid_phone_number));

        mCountryListSpinner.performClick();
        assertEquals(mErrorEditText.getText(), "");
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testVerifyPhoneNumberInvalidPhoneException_showsInlineError() {
        reset(AuthHelperShadow.sPhoneAuthProvider);

        mActivity.verifyPhoneNumber(PHONE, false);
        AlertDialog alert = ShadowAlertDialog.getLatestAlertDialog();
        ShadowAlertDialog sAlert = shadowOf(alert);
        //was dialog displayed
        assertEquals(mActivity.getString(R.string.fui_verifying), sAlert.getMessage());

        //was upstream method invoked
        verify(AuthHelperShadow.sPhoneAuthProvider).verifyPhoneNumber(
                eq(PHONE),
                eq(AUTO_RETRIEVAL_TIMEOUT_MILLIS),
                eq(TimeUnit.MILLISECONDS),
                eq(mActivity),
                callbacksArgumentCaptor.capture(),
                (PhoneAuthProvider.ForceResendingToken) isNull());

        PhoneAuthProvider.OnVerificationStateChangedCallbacks onVerificationStateChangedCallbacks
                = callbacksArgumentCaptor.getValue();
        onVerificationStateChangedCallbacks.onVerificationFailed(
                new FirebaseAuthException(
                        FirebaseAuthError.ERROR_INVALID_PHONE_NUMBER.toString(),
                        "any_message"));

        //was error displayed
        assertEquals(mErrorEditText.getText(), mActivity.getString(R.string.fui_invalid_phone_number));
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testVerifyPhoneNumberNoMsgException_showsAlertDialog() {
        reset(AuthHelperShadow.sPhoneAuthProvider);

        mActivity.verifyPhoneNumber(PHONE, false);
        verify(AuthHelperShadow.sPhoneAuthProvider).verifyPhoneNumber(
                eq(PHONE),
                eq(AUTO_RETRIEVAL_TIMEOUT_MILLIS),
                eq(TimeUnit.MILLISECONDS),
                eq(mActivity),
                callbacksArgumentCaptor.capture(),
                (PhoneAuthProvider.ForceResendingToken) isNull());

        PhoneAuthProvider.OnVerificationStateChangedCallbacks onVerificationStateChangedCallbacks
                = callbacksArgumentCaptor.getValue();

        onVerificationStateChangedCallbacks.onVerificationFailed(
                new FirebaseAuthException("some_code", "custom_message"));
        assertTrue(mActivity.getAlertDialog().isShowing());
        assertEquals(FirebaseAuthError.ERROR_UNKNOWN.getDescription(), getAlertDialogMessage());
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testVerifyPhoneNumber_success() {
        reset(AuthHelperShadow.sPhoneAuthProvider);
        testSendConfirmationCode();
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSubmitCode_badCodeShowsAlertDialog() {
        reset(AuthHelperShadow.sPhoneAuthProvider);
        when(AuthHelperShadow.sFirebaseAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        null, true,
                        new FirebaseAuthInvalidCredentialsException(
                                FirebaseAuthError.ERROR_INVALID_VERIFICATION_CODE.toString(),
                                "any_msg")));
        testSendConfirmationCode();
        SpacedEditText mConfirmationCodeEditText = (SpacedEditText) mActivity.findViewById(R.id.confirmation_code);
        Button mSubmitConfirmationButton = (Button) mActivity.findViewById(R.id.submit_confirmation_code);

        mConfirmationCodeEditText.setText("123456");
        mSubmitConfirmationButton.performClick();
        assertEquals(mActivity.getString(R.string.fui_incorrect_code_dialog_body),
                     getAlertDialogMessage());

        //test bad code cleared on clicking OK in alert
        android.support.v7.app.AlertDialog a = mActivity.getAlertDialog();
        Button ok = (Button) a.findViewById(android.R.id.button1);
        ok.performClick();
        assertEquals("- - - - - -", mConfirmationCodeEditText.getText().toString());
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testresendCode_invokesUpstream() {
        reset(AuthHelperShadow.sPhoneAuthProvider);
        testSendConfirmationCode();

        //test resend code invisible
        TextView r = (TextView) mActivity.findViewById(R.id.resend_code);
        assertEquals(View.GONE, r.getVisibility());

        //assert resend visible after timeout
        SubmitConfirmationCodeFragment fragment = (SubmitConfirmationCodeFragment) mActivity
                .getSupportFragmentManager().findFragmentByTag(SubmitConfirmationCodeFragment.TAG);
        fragment.getCountdownTimer().onFinish();
        assertEquals(View.VISIBLE, r.getVisibility());
        r.performClick();

        //assert resend invisible
        assertEquals(View.GONE, r.getVisibility());

        //verify resend code was called
        verify(AuthHelperShadow.sPhoneAuthProvider).verifyPhoneNumber(
                eq(PHONE),
                eq(AUTO_RETRIEVAL_TIMEOUT_MILLIS),
                eq(TimeUnit.MILLISECONDS),
                eq(mActivity),
                callbacksArgumentCaptor.capture(),
                eq(forceResendingToken));
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testAutoVerify() {
        reset(AuthHelperShadow.sPhoneAuthProvider);
        reset(AuthHelperShadow.sSaveSmartLock);
        reset(AuthHelperShadow.sFirebaseAuth);

        when(AuthHelperShadow.sFirebaseUser.getPhoneNumber()).thenReturn(PHONE);
        when(AuthHelperShadow.sFirebaseUser.getEmail()).thenReturn(null);
        when(AuthHelperShadow.sFirebaseAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));
        mActivity.verifyPhoneNumber(PHONE, false);
        verify(AuthHelperShadow.sPhoneAuthProvider).verifyPhoneNumber(
                eq(PHONE),
                eq(AUTO_RETRIEVAL_TIMEOUT_MILLIS),
                eq(TimeUnit.MILLISECONDS),
                eq(mActivity),
                callbacksArgumentCaptor.capture(),
                (PhoneAuthProvider.ForceResendingToken) isNull());

        PhoneAuthProvider.OnVerificationStateChangedCallbacks onVerificationStateChangedCallbacks
                = callbacksArgumentCaptor.getValue();

        onVerificationStateChangedCallbacks.onVerificationCompleted(credential);
        verify(AuthHelperShadow.sFirebaseAuth).signInWithCredential(any(AuthCredential.class));
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSMSAutoRetrieval() {
        reset(AuthHelperShadow.sPhoneAuthProvider);
        reset(AuthHelperShadow.sSaveSmartLock);
        when(credential.getSmsCode()).thenReturn("123456");

        when(AuthHelperShadow.sFirebaseUser.getPhoneNumber()).thenReturn(PHONE);
        when(AuthHelperShadow.sFirebaseUser.getEmail()).thenReturn(null);

        when(AuthHelperShadow.sFirebaseAuth.signInWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                testSendConfirmationCode();
        callbacks.onVerificationCompleted(credential);
        SpacedEditText mConfirmationCodeEditText = (SpacedEditText) mActivity.findViewById(R.id.confirmation_code);

        //verify confirmation code set
        assertEquals("1 2 3 4 5 6", mConfirmationCodeEditText.getText().toString());
        //verify credential saves
        verify(AuthHelperShadow.sFirebaseAuth).signInWithCredential(credential);
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testEditPhoneNumber_togglesFragments() {
        reset(AuthHelperShadow.sPhoneAuthProvider);
        testSendConfirmationCode();
        TextView mEditPhoneTextView = (TextView) mActivity.findViewById(R.id.edit_phone_number);
        mEditPhoneTextView.performClick();
        VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                mActivity.getSupportFragmentManager()
                        .findFragmentByTag(VerifyPhoneNumberFragment.TAG);
        SubmitConfirmationCodeFragment submitConfirmationCodeFragment =
                (SubmitConfirmationCodeFragment) mActivity.getSupportFragmentManager()
                        .findFragmentByTag(SubmitConfirmationCodeFragment.TAG);

        assertNotNull(verifyPhoneNumberFragment);

        assertNull(submitConfirmationCodeFragment);

    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks testSendConfirmationCode() {
        mActivity.verifyPhoneNumber(PHONE, false);
        verify(AuthHelperShadow.sPhoneAuthProvider).verifyPhoneNumber(
                eq(PHONE),
                eq(AUTO_RETRIEVAL_TIMEOUT_MILLIS),
                eq(TimeUnit.MILLISECONDS),
                eq(mActivity),
                callbacksArgumentCaptor.capture(),
                (PhoneAuthProvider.ForceResendingToken) isNull());

        PhoneAuthProvider.OnVerificationStateChangedCallbacks onVerificationStateChangedCallbacks
                = callbacksArgumentCaptor.getValue();

        onVerificationStateChangedCallbacks.onCodeSent(verificationId, forceResendingToken);

        //Force postDelayed runnables to completed on looper
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        SubmitConfirmationCodeFragment fragment = (SubmitConfirmationCodeFragment) mActivity
                .getSupportFragmentManager().findFragmentByTag(SubmitConfirmationCodeFragment.TAG);
        assertNotNull(fragment);

        SpacedEditText mConfirmationCodeEditText = (SpacedEditText) mActivity
                .findViewById(R.id.confirmation_code);
        assertTrue(mConfirmationCodeEditText.isFocused());

        return onVerificationStateChangedCallbacks;
    }

    private String getAlertDialogMessage() {
        android.support.v7.app.AlertDialog a = mActivity.getAlertDialog();
        assertTrue(a.isShowing());
        return ((TextView) (a.findViewById(android.R.id.message))).getText().toString();
    }
}
