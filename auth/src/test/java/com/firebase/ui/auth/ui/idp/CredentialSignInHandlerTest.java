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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.provider.IDPResponse;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FakeProviderQueryResult;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.email.SignInActivity;
import com.firebase.ui.auth.util.CredentialsAPI;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.firebase.ui.auth.util.SmartLock;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, shadows = {ActivityHelperShadow.class})
public class CredentialSignInHandlerTest {
    private static final int RC_ACCOUNT_LINK = 3;
    private static final int RC_SAVE_CREDENTIALS = 4;
    private static final String LINKING_ERROR = "ERROR_TEST_LINKING";
    private static final String LINKING_EXPLANATION = "Test explanation";

    @Mock
    private CredentialsAPI mCredentialsAPI;
    private FirebaseApp mFirebaseApp;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFirebaseApp = TestHelper.initializeApp(RuntimeEnvironment.application);
        when(mCredentialsAPI.isPlayServicesAvailable()).thenReturn(true);
        when(mCredentialsAPI.isCredentialsAvailable()).thenReturn(true);
        when(mCredentialsAPI.isAutoSignInAvailable()).thenReturn(true);

        PlayServicesHelper.sApiAvailability = TestHelper.makeMockGoogleApiAvailability();
    }

    @Test
    public void testSignInSucceeded() {
        AppCompatBase mockActivity = mock(AppCompatBase.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        IDPResponse idpResponse = new IDPResponse(
                GoogleAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                new Bundle());
        SmartLock smartLock = mock(SmartLock.class);
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                mockActivityHelper,
                smartLock,
                RC_ACCOUNT_LINK,
                idpResponse);
        Context mockContext = mock(Context.class);

        // Build basic flow parameters
        FlowParameters flowParams = AuthUI.getInstance(mFirebaseApp)
                .createSignInIntentBuilder()
                .build()
                .getParcelableExtra(ExtraConstants.EXTRA_FLOW_PARAMS);

        Task signInTask = Tasks.forResult(new FakeAuthResult(mockFirebaseUser));
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(flowParams);
        credentialSignInHandler.onComplete(signInTask);

        verify(smartLock).saveCredentialsOrFinish(mockActivity, mockActivityHelper, credentialSignInHandler, mockFirebaseUser, null, GoogleAuthProvider.PROVIDER_ID);

        SmartLock smart = new SmartLock();
        SmartLock.SmartLockResultListener resultListener = mock(SmartLock.SmartLockResultListener.class);

        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                latch.countDown();
                return null;
            }
        }).when(resultListener).onCredentialsSaved(Activity.RESULT_OK);


        SignInActivity s = createActivity();

        try {
            smart.saveCredentialsOrFinish(s,
                                          new ActivityHelper(mockActivity, mockActivity.getIntent()),
                                          resultListener,
                                          mockFirebaseUser,
                                          TestConstants.PASSWORD,
                                          GoogleAuthProvider.PROVIDER_ID);
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SignInActivity createActivity() {
        Intent startIntent = SignInActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        Collections.<String>emptyList()),
                null);
        return Robolectric
                .buildActivity(SignInActivity.class)
                .withIntent(startIntent)
                .create()
                .visible()
                .get();
    }

    @Test
    public void testSignInFailed_withFacebookAlreadyLinked() {
        AppCompatBase mockActivity = mock(AppCompatBase.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        IDPResponse idpResponse = new IDPResponse(
                GoogleAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                new Bundle());
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                mockActivityHelper,
                new SmartLock(),
                RC_ACCOUNT_LINK,
                idpResponse);

        Context mockContext = mock(Context.class);
        FlowParameters mockFlowParams = mock(FlowParameters.class);
        when(mockActivityHelper.getFirebaseAuth()).thenReturn(mockFirebaseAuth);
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(mockFlowParams);

        // pretend the account has Facebook linked already
        when(mockFirebaseAuth.fetchProvidersForEmail(TestConstants.EMAIL)).thenReturn(
                new AutoCompleteTask<ProviderQueryResult>(
                        new FakeProviderQueryResult(
                                Arrays.asList(FacebookAuthProvider.PROVIDER_ID)), true, null));

        // pretend there was already an account with this email
        Task exceptionTask = Tasks.forException(
                new FirebaseAuthUserCollisionException(LINKING_ERROR, LINKING_EXPLANATION));
        credentialSignInHandler.onComplete(exceptionTask);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivity).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(RC_ACCOUNT_LINK, (int) intCaptor.getValue());
        assertEquals(
                WelcomeBackIDPPrompt.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
        assertEquals(
                FacebookAuthProvider.PROVIDER_ID,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_PROVIDER));

    }

    @Test
    public void testSignInFailed_withPasswordAccountAlreadyLinked() {
        AppCompatBase mockActivity = mock(AppCompatBase.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        IDPResponse idpResponse = new IDPResponse(
                GoogleAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                new Bundle());
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                mockActivityHelper,
                new SmartLock(),
                RC_ACCOUNT_LINK,
                idpResponse);

        Context mockContext = mock(Context.class);
        Task mockTask = mock(Task.class);
        FlowParameters mockFlowParams = mock(FlowParameters.class);

        // pretend there was already an account with this email
        when(mockTask.getException()).thenReturn(
                new FirebaseAuthUserCollisionException(LINKING_ERROR, LINKING_EXPLANATION));
        when(mockActivityHelper.getFirebaseAuth()).thenReturn(mockFirebaseAuth);
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(mockFlowParams);

        // pretend the account has a Password account linked already
        when(mockFirebaseAuth.fetchProvidersForEmail(TestConstants.EMAIL)).thenReturn(
                new AutoCompleteTask<ProviderQueryResult>(
                        new FakeProviderQueryResult(
                                Arrays.asList(EmailAuthProvider.PROVIDER_ID)), true, null));


        credentialSignInHandler.onComplete(mockTask);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivity).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(RC_ACCOUNT_LINK, (int) intCaptor.getValue());
        assertEquals(
                WelcomeBackPasswordPrompt.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                ((IDPResponse) capturedIntent
                        .getExtras()
                        .getParcelable(ExtraConstants.EXTRA_IDP_RESPONSE))
                        .getEmail());
    }
}
