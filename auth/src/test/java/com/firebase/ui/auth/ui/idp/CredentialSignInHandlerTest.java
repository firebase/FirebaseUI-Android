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

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.account_link.WelcomeBackIDPPrompt;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

import java.util.Arrays;


@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ActivityHelperShadow.class})
public class CredentialSignInHandlerTest {
    private static final int RC_ACCOUNT_LINK = 3;
    private static final int RC_SAVE_CREDENTIALS = 4;
    private static final String LINKING_ERROR = "ERROR_TEST_LINKING";
    private static final String LINKING_EXPLANATION = "Test explanation";

    @Test
    public void testSignInSucceeded() {
        Activity mockActivity = mock(Activity.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        IDPResponse idpResponse = new IDPResponse(
                GoogleAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                new Bundle());
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                mockActivityHelper,
                RC_ACCOUNT_LINK,
                RC_SAVE_CREDENTIALS,
                idpResponse);
        Context mockContext = mock(Context.class);
        FlowParameters mockFlowParams = mock(FlowParameters.class);
        Task signInTask = Tasks.forResult(new FakeAuthResult(mockFirebaseUser));
        when(mockActivityHelper.getApplicationContext()).thenReturn(mockContext);
        when(mockActivityHelper.getFlowParams()).thenReturn(mockFlowParams);
        credentialSignInHandler.onComplete(signInTask);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivity).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals(RC_SAVE_CREDENTIALS, (int) intCaptor.getValue());
        assertEquals(
                SaveCredentialsActivity.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
        assertEquals(
                TestConstants.NAME,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_NAME));
        assertEquals(
                TestConstants.PHOTO_URL,
                capturedIntent.getExtras().getString(ExtraConstants.EXTRA_PROFILE_PICTURE_URI));
    }

    @Test
    public void testSignInFailed_withFacebookAlreadyLinked() {
        Activity mockActivity = mock(Activity.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        IDPResponse idpResponse = new IDPResponse(
                GoogleAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                new Bundle());
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                mockActivityHelper,
                RC_ACCOUNT_LINK,
                RC_SAVE_CREDENTIALS,
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
        Activity mockActivity = mock(Activity.class);
        ActivityHelper mockActivityHelper = mock(ActivityHelper.class);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        IDPResponse idpResponse = new IDPResponse(
                GoogleAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                new Bundle());
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                mockActivityHelper,
                RC_ACCOUNT_LINK,
                RC_SAVE_CREDENTIALS,
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
                ((IDPResponse) capturedIntent.getExtras().getParcelable(ExtraConstants
                        .EXTRA_IDP_RESPONSE)).getEmail());
    }
}
