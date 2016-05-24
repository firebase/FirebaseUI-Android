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

package com.firebase.ui.auth.ui;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.idp.IDPSignInContainerActivity;
import com.firebase.ui.auth.util.CredentialsAPI;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ActivityHelperShadow.class}, sdk = 21)
public class ChooseAccountActivityTest {
    private FirebaseApp mFirebaseApp;
    @Mock private CredentialsAPI mCredentialsAPI;
    @Mock private FirebaseAuth mFirebaseAuth;
    @Mock private ActivityHelper mActivityHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFirebaseApp = TestHelper.initializeApp(RuntimeEnvironment.application);
        when(mCredentialsAPI.isPlayServicesAvailable()).thenReturn(true);
        when(mCredentialsAPI.isCredentialsAvailable()).thenReturn(true);
        when(mCredentialsAPI.isAutoSignInAvailable()).thenReturn(true);

    }

    private Intent createStartIntent() {
        return AuthUI
                .getInstance(mFirebaseApp).createSignInIntentBuilder()
                .setProviders(AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER)
                .build();
    }

    @Test
    public void testAutoSignInWithSavedUsernameAndPassword_signsIn() {
        ChooseAccountActivity chooseAccountActivity =
                Robolectric.buildActivity(ChooseAccountActivity.class)
                        .withIntent(createStartIntent()).create().get();
        when(mCredentialsAPI.getEmailFromCredential()).thenReturn(TestConstants.EMAIL);
        when(mCredentialsAPI.getPasswordFromCredential()).thenReturn(TestConstants.PASSWORD);
        when(mCredentialsAPI.getAccountTypeFromCredential()).thenReturn(
                EmailAuthProvider.PROVIDER_ID);

        when(mActivityHelper.getFirebaseAuth()).thenReturn(mFirebaseAuth);
        when(mFirebaseAuth.signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD))
                .thenReturn(
                        new AutoCompleteTask<AuthResult>(
                                new FakeAuthResult(mock(FirebaseUser.class)), true, null));

        chooseAccountActivity.onCredentialsApiConnected(mCredentialsAPI, mActivityHelper);

        verify(mFirebaseAuth).signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD);

        assertTrue(Shadows.shadowOf(chooseAccountActivity).isFinishing());
    }

    @Test
    public void testAutoSignInWithSavedIdp_redirectsToIdpSignIn() {
        Intent startIntent = createStartIntent();
        ChooseAccountActivity chooseAccountActivity =
                Robolectric.buildActivity(ChooseAccountActivity.class)
                        .withIntent(startIntent).create().get();
        when(mCredentialsAPI.getEmailFromCredential()).thenReturn(TestConstants.EMAIL);
        when(mCredentialsAPI.getPasswordFromCredential()).thenReturn(null);
        when(mCredentialsAPI.getAccountTypeFromCredential()).thenReturn(
                IdentityProviders.GOOGLE);
        when(mActivityHelper.getFlowParams()).thenReturn(
                (FlowParameters) startIntent.getParcelableExtra(ExtraConstants.EXTRA_FLOW_PARAMS));

        chooseAccountActivity.onCredentialsApiConnected(mCredentialsAPI, mActivityHelper);

        ShadowActivity.IntentForResult nextIntent = Shadows
                .shadowOf(chooseAccountActivity)
                .getNextStartedActivityForResult();

        assertEquals(
                IDPSignInContainerActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName()
        );
        assertEquals(
                TestConstants.EMAIL,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
        assertEquals(
                GoogleAuthProvider.PROVIDER_ID,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_PROVIDER)
        );
    }
}
