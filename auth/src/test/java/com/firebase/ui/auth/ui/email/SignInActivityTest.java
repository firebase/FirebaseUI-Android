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
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.util.PlayServicesHelper;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SignInActivityTest {

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
        PlayServicesHelper.sApiAvailability = TestHelper.makeMockGoogleApiAvailability();
    }

    private SignInActivity createActivity() {
        Intent startIntent = SignInActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        RuntimeEnvironment.application,
                        Collections.<String>emptyList()),
                null);
        return Robolectric.buildActivity(SignInActivity.class).withIntent(startIntent)
                .create().visible().get();

    }

    @Test
    public void testSignInButton_validatesFields() {
        SignInActivity signInActivity = createActivity();
        Button signIn = (Button) signInActivity.findViewById(R.id.button_done);
        signIn.performClick();
        TextInputLayout emailLayout =
                (TextInputLayout) signInActivity.findViewById(R.id.email_layout);
        TextInputLayout passwordLayout =
                (TextInputLayout) signInActivity.findViewById(R.id.password_layout);

        assertEquals(
                signInActivity.getResources().getString(R.string.missing_email_address),
                emailLayout.getError());
        assertEquals(
                signInActivity.getResources().getString(R.string.required_field),
                passwordLayout.getError());

        // should block and not start a new activity
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(signInActivity).getNextStartedActivityForResult();
        assertNull(nextIntent);
    }

    @Test
    @Config(shadows = {ActivityHelperShadow.class, FirebaseAuthWrapperImplShadow.class})
    public void testSignInButton_signsInAndSavesCredentials() {
        SignInActivity signInActivity = createActivity();
        EditText emailField = (EditText) signInActivity.findViewById(R.id.email);
        EditText passwordField = (EditText) signInActivity.findViewById(R.id.password);
        emailField.setText(TestConstants.EMAIL);
        passwordField.setText(TestConstants.PASSWORD);

        FirebaseUser mockFirebaseUser = mock(FirebaseUser.class);

        when(ActivityHelperShadow.firebaseAuth.signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD)).thenReturn(
                    new AutoCompleteTask<>(new FakeAuthResult(mockFirebaseUser), true, null));
        when(mockFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
        when(mockFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);

        Button signIn = (Button) signInActivity.findViewById(R.id.button_done);
        signIn.performClick();

        verify(ActivityHelperShadow.firebaseAuth).signInWithEmailAndPassword(
                TestConstants.EMAIL,
                TestConstants.PASSWORD);

        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(signInActivity).getNextStartedActivityForResult();
        assertEquals(
                SaveCredentialsActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName()
        );
        assertEquals(
                TestConstants.EMAIL,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_EMAIL)
        );
        assertEquals(
                TestConstants.PASSWORD,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_PASSWORD)
        );
        assertEquals(
                TestConstants.NAME,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_NAME)
        );
    }
}
