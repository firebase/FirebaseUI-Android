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
import android.support.design.widget.TextInputLayout;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.testhelpers.ActivityHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.BaseHelperShadow;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackPasswordPrompt;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;

import static com.firebase.ui.auth.testhelpers.TestHelper.verifySmartLockSave;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class WelcomeBackPasswordPromptTest {
    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
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
    public void testSignInButton_validatesFields() {
        WelcomeBackPasswordPrompt welcomeBack = createActivity();
        Button signIn = (Button) welcomeBack.findViewById(R.id.button_done);
        signIn.performClick();
        TextInputLayout passwordLayout =
                (TextInputLayout) welcomeBack.findViewById(R.id.password_layout);

        assertEquals(
                welcomeBack.getString(R.string.required_field),
                passwordLayout.getError().toString());

        // should block and not start a new activity
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(welcomeBack).getNextStartedActivityForResult();
        assertNull(nextIntent);
    }

    @Test
    @Config(shadows = {BaseHelperShadow.class, ActivityHelperShadow.class})
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

        verifySmartLockSave(EmailAuthProvider.PROVIDER_ID,
                            TestConstants.EMAIL,
                            TestConstants.PASSWORD);
    }
}
