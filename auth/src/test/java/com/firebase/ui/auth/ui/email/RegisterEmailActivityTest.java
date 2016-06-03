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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Arrays;


@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class RegisterEmailActivityTest {

    private RegisterEmailActivity createActivity(String email) {
        Intent startIntent = SignInNoPasswordActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        RuntimeEnvironment.application,
                        Arrays.asList(AuthUI.EMAIL_PROVIDER)),
                email);
        return Robolectric.buildActivity(RegisterEmailActivity.class)
                .withIntent(startIntent).create().visible().get();
    }

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    @Test
    public void testSignUpButton_validatesFields() {
        RegisterEmailActivity registerEmailActivity = createActivity(TestConstants.EMAIL);
        Button button = (Button) registerEmailActivity.findViewById(R.id.button_create);
        button.performClick();

        TextInputLayout nameLayout = (TextInputLayout) registerEmailActivity
                .findViewById(R.id.name_layout);

        TextInputLayout passwordLayout = (TextInputLayout) registerEmailActivity
                .findViewById(R.id.password_layout);

        assertEquals(
                nameLayout.getError().toString(),
                registerEmailActivity.getString(R.string.required_field));
        assertEquals(
                passwordLayout.getError().toString(),
                String.format(registerEmailActivity.getString(R.string.password_length),
                        registerEmailActivity.getResources().getInteger(
                                R.integer.min_password_length)));
    }

    @Test
    @Config(shadows = {ActivityHelperShadow.class, FirebaseAuthWrapperImplShadow.class})
    public void testSignupButton_successfulRegisterationShouldContinueToSaveCredentials() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
        RegisterEmailActivity registerEmailActivity = createActivity(TestConstants.EMAIL);

        EditText name = (EditText) registerEmailActivity.findViewById(R.id.name);
        EditText password = (EditText) registerEmailActivity.findViewById(R.id.password);
        name.setText(TestConstants.NAME);
        password.setText(TestConstants.PASSWORD);

        FirebaseUser mockFirebaseUser = Mockito.mock(FirebaseUser.class);
        when(mockFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
        when(mockFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
        when(mockFirebaseUser.updateProfile((UserProfileChangeRequest) Mockito.anyObject()))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));

        when(ActivityHelperShadow.firebaseAuth
                .createUserWithEmailAndPassword(
                        TestConstants.EMAIL,
                        TestConstants.PASSWORD))
                .thenReturn(
                        new AutoCompleteTask<>(
                                new FakeAuthResult(mockFirebaseUser),
                                true,
                                null));


        Button button = (Button) registerEmailActivity.findViewById(R.id.button_create);
        button.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(registerEmailActivity);

        ShadowActivity.IntentForResult nextIntent =
                shadowActivity.getNextStartedActivityForResult();

        assertNotNull(nextIntent);
        assertEquals(
                nextIntent.intent.getComponent().getClassName(),
                SaveCredentialsActivity.class.getName());
        assertEquals(
                TestConstants.EMAIL,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
        assertEquals(
                TestConstants.PASSWORD,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_PASSWORD));
        assertEquals(
                TestConstants.NAME,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_NAME));
    }
}
