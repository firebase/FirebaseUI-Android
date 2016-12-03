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

package com.firebase.ui.auth.ui.email;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.BaseHelperShadow;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.account_link.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static com.firebase.ui.auth.test_helpers.TestHelper.verifySmartLockSave;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class WelcomeBackPasswordPromptTest {
    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
        PlayServicesHelper.sApiAvailability = TestHelper.makeMockGoogleApiAvailability();
    }

    private WelcomeBackPasswordPrompt createActivity() {
        Intent startIntent = WelcomeBackPasswordPrompt.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(Collections.<String>emptyList()),
                new IdpResponse(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL));
        return Robolectric
                .buildActivity(WelcomeBackPasswordPrompt.class)
                .withIntent(startIntent)
                .create()
                .visible()
                .get();
    }

    @Test
    @Config(shadows = {BaseHelperShadow.class, ActivityHelperShadow.class, FirebaseAuthWrapperImplShadow.class})
    public void testSignInButton_signsInAndSavesCredentials() {
        // initialize mocks
        new ActivityHelperShadow();
        reset(ActivityHelperShadow.sSaveSmartLock);

        WelcomeBackPasswordPrompt welcomeBackActivity = createActivity();
        EditText passwordField = (EditText) welcomeBackActivity.findViewById(R.id.password);
        passwordField.setText(TestConstants.PASSWORD);

        FirebaseUser mockFirebaseUser = mock(FirebaseUser.class);

        when(ActivityHelperShadow.sFirebaseAuth.signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD)).thenReturn(
                    new AutoCompleteTask<AuthResult>(new FakeAuthResult(mockFirebaseUser), true, null));
        when(mockFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
        when(mockFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);

        Button signIn = (Button) welcomeBackActivity.findViewById(R.id.button_done);
        signIn.performClick();

        verify(ActivityHelperShadow.sFirebaseAuth).signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD);

        verifySmartLockSave(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL, TestConstants.PASSWORD);
    }
}
