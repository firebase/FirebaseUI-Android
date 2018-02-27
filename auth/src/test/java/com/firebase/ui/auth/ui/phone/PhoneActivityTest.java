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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.testhelpers.AuthHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.FirebaseAuthError;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.firebase.ui.auth.ui.phone.PhoneActivity.AUTO_RETRIEVAL_TIMEOUT_MILLIS;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.CA_COUNTRY_CODE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.CA_ISO2;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.PHONE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.PHONE_NO_COUNTRY_CODE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.YE_COUNTRY_CODE;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.YE_RAW_PHONE;
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

@RunWith(RobolectricTestRunner.class)
public class PhoneActivityTest {
    private PhoneActivity mActivity;
    private TextInputLayout mPhoneLayout;
    private Button mSendCodeButton;
    private EditText mPhoneEditText;
    private CountryListSpinner mCountryListSpinner;

    @Captor
    ArgumentCaptor<PhoneAuthProvider.OnVerificationStateChangedCallbacks> callbacksArgumentCaptor;
    @Mock
    PhoneAuthProvider.ForceResendingToken forceResendingToken;
    @Mock
    PhoneAuthCredential credential;

    private String verificationId = "hjksdf737hc";

    private PhoneActivity createActivity() {
        Intent startIntent = PhoneActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        Collections.singletonList(PhoneAuthProvider.PROVIDER_ID)), null);
        return Robolectric.buildActivity(PhoneActivity.class, startIntent)
                .create(new Bundle()).start().visible().get();
    }

    @Before
    public void setUp() {
        TestHelper.initialize();
        initMocks(this);
        mActivity = createActivity();
        mPhoneEditText = mActivity.findViewById(R.id.phone_number);
        mPhoneLayout = mActivity.findViewById(R.id.phone_layout);
        mSendCodeButton = mActivity.findViewById(R.id.send_code);
        mCountryListSpinner = mActivity.findViewById(R.id.country_list);
    }

    @Test
    public void testDefaultFullPhoneNumber_prePopulatesPhoneNumberInBundle() {
        Bundle fullPhoneParams = new Bundle();
        fullPhoneParams.putString(ExtraConstants.EXTRA_PHONE, YE_RAW_PHONE);
        Intent startIntent = PhoneActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        Collections.singletonList(PhoneAuthProvider.PROVIDER_ID)),
                fullPhoneParams);

        mActivity = Robolectric.buildActivity(PhoneActivity.class, startIntent)
                .create(new Bundle()).start().visible().get();

        VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                mActivity.getSupportFragmentManager()
                        .findFragmentByTag(VerifyPhoneNumberFragment.TAG);
        assertNotNull(verifyPhoneNumberFragment);
        mPhoneEditText = mActivity.findViewById(R.id.phone_number);
        mCountryListSpinner = mActivity.findViewById(R.id.country_list);

        assertEquals(PHONE_NO_COUNTRY_CODE, mPhoneEditText.getText().toString());
        assertEquals(YE_COUNTRY_CODE,
                String.valueOf(((CountryInfo) mCountryListSpinner.getTag()).getCountryCode()));
    }

    @Test
    public void testDefaultCountryCodeAndNationalNumber_prePopulatesPhoneNumberInBundle() {
        Bundle phoneParams = new Bundle();
        phoneParams.putString(ExtraConstants.EXTRA_COUNTRY_ISO, CA_ISO2);
        phoneParams.putString(ExtraConstants.EXTRA_NATIONAL_NUMBER, PHONE_NO_COUNTRY_CODE);
        Intent startIntent = PhoneActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        Collections.singletonList(PhoneAuthProvider.PROVIDER_ID)),
                phoneParams);

        mActivity = Robolectric.buildActivity(PhoneActivity.class, startIntent)
                .create(new Bundle()).start().visible().get();

        VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                mActivity.getSupportFragmentManager()
                        .findFragmentByTag(VerifyPhoneNumberFragment.TAG);
        assertNotNull(verifyPhoneNumberFragment);
        mPhoneEditText = mActivity.findViewById(R.id.phone_number);
        mCountryListSpinner = mActivity.findViewById(R.id.country_list);

        assertEquals(PHONE_NO_COUNTRY_CODE, mPhoneEditText.getText().toString());
        assertEquals(CA_COUNTRY_CODE,
                String.valueOf(((CountryInfo) mCountryListSpinner.getTag()).getCountryCode()));
        assertEquals(new Locale("", CA_ISO2),
                ((CountryInfo) mCountryListSpinner.getTag()).getLocale());
    }

    @Test
    public void testBadPhoneNumber_showsInlineError() {
        VerifyPhoneNumberFragment verifyPhoneNumberFragment = (VerifyPhoneNumberFragment)
                mActivity.getSupportFragmentManager()
                        .findFragmentByTag(VerifyPhoneNumberFragment.TAG);
        assertNotNull(verifyPhoneNumberFragment);

        mSendCodeButton.performClick();
        assertEquals(mPhoneLayout.getError(), mActivity.getString(R.string.fui_invalid_phone_number));

        mCountryListSpinner.performClick();
        assertNull(mPhoneLayout.getError());
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testVerifyPhoneNumberInvalidPhoneException_showsInlineError() {
        reset(AuthHelperShadow.getPhoneAuthProvider());

        mActivity.verifyPhoneNumber(PHONE, false);
        //was dialog displayed
        assertEquals(
                mActivity.getString(R.string.fui_verifying),
                mActivity.mProgressDialog.mMessageView.getText());

        //was upstream method invoked
        verify(AuthHelperShadow.getPhoneAuthProvider()).verifyPhoneNumber(
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
        assertEquals(mPhoneLayout.getError(), mActivity.getString(R.string.fui_invalid_phone_number));
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testVerifyPhoneNumberNoMsgException_showsAlertDialog() {
        reset(AuthHelperShadow.getPhoneAuthProvider());

        mActivity.verifyPhoneNumber(PHONE, false);
        verify(AuthHelperShadow.getPhoneAuthProvider()).verifyPhoneNumber(
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
        assertEquals(RuntimeEnvironment.application.getString(R.string.fui_error_unknown),
                getAlertDialogMessage());
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testVerifyPhoneNumber_success() {
        reset(AuthHelperShadow.getPhoneAuthProvider());
        testSendConfirmationCode();
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSubmitCode_badCodeShowsAlertDialog() {
        reset(AuthHelperShadow.getPhoneAuthProvider());
        when(AuthHelperShadow.getFirebaseAuth().signInWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        null, true,
                        new FirebaseAuthInvalidCredentialsException(
                                FirebaseAuthError.ERROR_INVALID_VERIFICATION_CODE.toString(),
                                "any_msg")));
        testSendConfirmationCode();
        SpacedEditText mConfirmationCodeEditText = mActivity.findViewById(R.id.confirmation_code);
        Button mSubmitConfirmationButton = mActivity.findViewById(R.id.submit_confirmation_code);

        mConfirmationCodeEditText.setText("123456");
        mSubmitConfirmationButton.performClick();
        assertEquals(mActivity.getString(R.string.fui_incorrect_code_dialog_body),
                     getAlertDialogMessage());

        //test bad code cleared on clicking OK in alert
        android.support.v7.app.AlertDialog a = mActivity.getAlertDialog();
        Button ok = a.findViewById(android.R.id.button1);
        ok.performClick();
        assertEquals("- - - - - -", mConfirmationCodeEditText.getText().toString());
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testresendCode_invokesUpstream() {
        reset(AuthHelperShadow.getPhoneAuthProvider());
        testSendConfirmationCode();

        //test resend code invisible
        TextView r = mActivity.findViewById(R.id.resend_code);
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
        verify(AuthHelperShadow.getPhoneAuthProvider()).verifyPhoneNumber(
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
        reset(AuthHelperShadow.getPhoneAuthProvider());
        reset(AuthHelperShadow.getFirebaseAuth());

        when(AuthHelperShadow.getCurrentUser().getPhoneNumber()).thenReturn(PHONE);
        when(AuthHelperShadow.getCurrentUser().getEmail()).thenReturn(null);
        when(AuthHelperShadow.getFirebaseAuth().signInWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));
        mActivity.verifyPhoneNumber(PHONE, false);
        verify(AuthHelperShadow.getPhoneAuthProvider()).verifyPhoneNumber(
                eq(PHONE),
                eq(AUTO_RETRIEVAL_TIMEOUT_MILLIS),
                eq(TimeUnit.MILLISECONDS),
                eq(mActivity),
                callbacksArgumentCaptor.capture(),
                (PhoneAuthProvider.ForceResendingToken) isNull());

        PhoneAuthProvider.OnVerificationStateChangedCallbacks onVerificationStateChangedCallbacks
                = callbacksArgumentCaptor.getValue();

        onVerificationStateChangedCallbacks.onVerificationCompleted(credential);
        verify(AuthHelperShadow.getFirebaseAuth()).signInWithCredential(any(AuthCredential.class));
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSMSAutoRetrieval() {
        reset(AuthHelperShadow.getPhoneAuthProvider());
        when(credential.getSmsCode()).thenReturn("123456");

        when(AuthHelperShadow.getCurrentUser().getPhoneNumber()).thenReturn(PHONE);
        when(AuthHelperShadow.getCurrentUser().getEmail()).thenReturn(null);

        when(AuthHelperShadow.getFirebaseAuth().signInWithCredential(any(AuthCredential.class)))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                testSendConfirmationCode();
        callbacks.onVerificationCompleted(credential);
        SpacedEditText mConfirmationCodeEditText = mActivity.findViewById(R.id.confirmation_code);

        //verify confirmation code set
        assertEquals("1 2 3 4 5 6", mConfirmationCodeEditText.getText().toString());
        //verify credential saves
        verify(AuthHelperShadow.getFirebaseAuth()).signInWithCredential(credential);
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testEditPhoneNumber_togglesFragments() {
        reset(AuthHelperShadow.getPhoneAuthProvider());
        testSendConfirmationCode();
        TextView mEditPhoneTextView = mActivity.findViewById(R.id.edit_phone_number);
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
        verify(AuthHelperShadow.getPhoneAuthProvider()).verifyPhoneNumber(
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

        SpacedEditText mConfirmationCodeEditText = mActivity
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
