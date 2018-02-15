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

import android.content.Intent;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.AuthHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeProviderQueryResult;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.ui.ProgressDialogHolder;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.util.AuthHelper;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class CredentialSignInHandlerTest {
    private static final int RC_ACCOUNT_LINK = 3;
    private static final String LINKING_ERROR = "ERROR_TEST_LINKING";
    private static final String LINKING_EXPLANATION = "Test explanation";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestHelper.initialize();
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSignInFailed_withFacebookAlreadyLinked() {
        AppCompatBase mockActivity = mock(AppCompatBase.class);
        IdpResponse idpResponse =
                new IdpResponse.Builder(
                        new User.Builder(GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                                .build())
                        .setToken(TestConstants.TOKEN)
                        .build();
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                RC_ACCOUNT_LINK,
                idpResponse);

        FlowParameters mockFlowParams = mock(FlowParameters.class);
        when(mockActivity.getFlowParams()).thenReturn(mockFlowParams);

        AuthHelper mockAuthHelper = mock(AuthHelper.class);
        when(mockActivity.getAuthHelper()).thenReturn(mockAuthHelper);
        AuthHelperShadow.getFirebaseAuth(); // Force static initialization
        when(mockAuthHelper.getFirebaseAuth()).thenReturn(AuthHelperShadow.getFirebaseAuth());

        ProgressDialogHolder mockHolder = mock(ProgressDialogHolder.class);
        when(mockActivity.getDialogHolder()).thenReturn(mockHolder);

        // pretend the account has Facebook linked already
        when(AuthHelperShadow.getFirebaseAuth().fetchProvidersForEmail(TestConstants.EMAIL)).thenReturn(
                new AutoCompleteTask<ProviderQueryResult>(
                        new FakeProviderQueryResult(
                                Arrays.asList(FacebookAuthProvider.PROVIDER_ID)), true, null));

        // pretend there was already an account with this email
        credentialSignInHandler.onFailure(
                new FirebaseAuthUserCollisionException(LINKING_ERROR, LINKING_EXPLANATION));
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockActivity).startActivityForResult(intentCaptor.capture(), intCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        User capturedUser = User.getUser(capturedIntent);
        assertEquals(RC_ACCOUNT_LINK, (int) intCaptor.getValue());
        assertEquals(
                WelcomeBackIdpPrompt.class.getName(),
                capturedIntent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                capturedUser.getEmail());
        assertEquals(
                FacebookAuthProvider.PROVIDER_ID,
                capturedUser.getProviderId());

    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSignInFailed_withPasswordAccountAlreadyLinked() {
        AppCompatBase mockActivity = mock(AppCompatBase.class);
        IdpResponse idpResponse =
                new IdpResponse.Builder(
                        new User.Builder(GoogleAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                                .build())
                        .setToken(TestConstants.TOKEN)
                        .build();
        CredentialSignInHandler credentialSignInHandler = new CredentialSignInHandler(
                mockActivity,
                RC_ACCOUNT_LINK,
                idpResponse);

        FlowParameters mockFlowParams = mock(FlowParameters.class);
        when(mockActivity.getFlowParams()).thenReturn(mockFlowParams);

        AuthHelper mockAuthHelper = mock(AuthHelper.class);
        when(mockActivity.getAuthHelper()).thenReturn(mockAuthHelper);
        AuthHelperShadow.getFirebaseAuth(); // Force static initialization
        when(mockAuthHelper.getFirebaseAuth()).thenReturn(AuthHelperShadow.getFirebaseAuth());

        ProgressDialogHolder mockHolder = mock(ProgressDialogHolder.class);
        when(mockActivity.getDialogHolder()).thenReturn(mockHolder);

        // pretend the account has a Password account linked already
        when(AuthHelperShadow.getFirebaseAuth().fetchProvidersForEmail(TestConstants.EMAIL)).thenReturn(
                new AutoCompleteTask<ProviderQueryResult>(
                        new FakeProviderQueryResult(
                                Arrays.asList(EmailAuthProvider.PROVIDER_ID)), true, null));

        // pretend there was already an account with this email
        Exception exception =
                new FirebaseAuthUserCollisionException(LINKING_ERROR, LINKING_EXPLANATION);

        credentialSignInHandler.onFailure(exception);
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
                IdpResponse.fromResultIntent(capturedIntent).getEmail());
    }
}
