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

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.idp.IdpSignInContainerActivity;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.firebase.ui.auth.util.smartlock.SignInDelegate;
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

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ActivityHelperShadow.class}, sdk = 23)
public class ChooseAccountActivityTest {
    private FirebaseApp mFirebaseApp;
    @Mock
    private FirebaseAuth mFirebaseAuth;
    @Mock
    private ActivityHelper mActivityHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFirebaseApp = TestHelper.initializeApp(RuntimeEnvironment.application);
        PlayServicesHelper.sApiAvailability = TestHelper.makeMockGoogleApiAvailability();

//        when(mCredentialsAPI.isPlayServicesAvailable()).thenReturn(true);
//        when(mCredentialsAPI.isCredentialsAvailable()).thenReturn(true);
//        when(mCredentialsAPI.isAutoSignInAvailable()).thenReturn(true);
    }

    @Test
    public void testAutoSignInWithSavedUsernameAndPassword_signsIn() {
        SignInDelegate signInDelegate = mock(SignInDelegate.class);

        when(mActivityHelper.getFirebaseAuth()).thenReturn(mFirebaseAuth);

        when(mFirebaseAuth.signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mock(FirebaseUser.class)), true, null));

        signInDelegate.redirectToIdpOrSignInWithEmailAndPassword(TestConstants.EMAIL,
                                                                 TestConstants.PASSWORD,
                                                                 EmailAuthProvider.PROVIDER_ID);

        verify(mFirebaseAuth).signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD);

        verify(signInDelegate).finish(100, new Intent());
    }

    @Test
    public void testAutoSignInWithSavedIdp_redirectsToIdpSignIn() {
        SignInDelegate signInDelegate = mock(DummySignInDelegate.class);

        signInDelegate.redirectToIdpOrSignInWithEmailAndPassword(TestConstants.EMAIL,
                                                                 null,
                                                                 IdentityProviders.GOOGLE);
    }

    private class DummySignInDelegate extends SignInDelegate {
        @Override
        public void finish(int resultCode, Intent data) {
            assertEquals(2121, resultCode);
            assertEquals(new Intent(), data);
        }
    }
}
