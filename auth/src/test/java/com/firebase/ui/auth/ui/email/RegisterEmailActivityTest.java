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
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.testhelpers.ActivityHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.BaseHelperShadow;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.User;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;


@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class RegisterEmailActivityTest {

    private RegisterEmailActivity createActivity() {
        Intent startIntent = RegisterEmailActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(Collections.singletonList(AuthUI.EMAIL_PROVIDER)));
        return Robolectric.buildActivity(RegisterEmailActivity.class)
                .withIntent(startIntent)
                .create(new Bundle())
                .start()
                .visible()
                .get();
    }

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    @Test
    public void testSignUpButton_validatesFields() {
        RegisterEmailActivity registerEmailActivity = createActivity();

        // Trigger RegisterEmailFragment (bypass check email)
        registerEmailActivity.onNewUser(new User.Builder(TestConstants.EMAIL).build());

        Button button = (Button) registerEmailActivity.findViewById(R.id.button_create);
        button.performClick();

        TextInputLayout nameLayout = (TextInputLayout)
                registerEmailActivity.findViewById(R.id.name_layout);
        TextInputLayout passwordLayout = (TextInputLayout)
                registerEmailActivity.findViewById(R.id.password_layout);

        assertEquals(
                registerEmailActivity.getString(R.string.required_field),
                nameLayout.getError().toString());
        assertEquals(
                String.format(
                        registerEmailActivity.getResources().getQuantityString(
                                R.plurals.error_weak_password,
                                R.integer.min_password_length),
                        registerEmailActivity.getResources()
                                .getInteger(R.integer.min_password_length)
                ),
                passwordLayout.getError().toString());
    }

    @Test
    @Config(shadows = {
            BaseHelperShadow.class,
            ActivityHelperShadow.class
    })
    public void testSignUpButton_successfulRegistrationShouldContinueToSaveCredentials() {
        // init mocks
        new BaseHelperShadow();
        reset(BaseHelperShadow.sSaveSmartLock);

        TestHelper.initializeApp(RuntimeEnvironment.application);
        RegisterEmailActivity registerEmailActivity = createActivity();

        // Trigger new user UI (bypassing check email)
        registerEmailActivity.onNewUser(new User.Builder(TestConstants.EMAIL)
                                                .setName(TestConstants.NAME)
                                                .setPhotoUri(TestConstants.PHOTO_URI)
                                                .build());

        EditText name = (EditText) registerEmailActivity.findViewById(R.id.name);
        EditText password = (EditText) registerEmailActivity.findViewById(R.id.password);
        name.setText(TestConstants.NAME);
        password.setText(TestConstants.PASSWORD);

        FirebaseUser mockFirebaseUser = Mockito.mock(FirebaseUser.class);
        when(mockFirebaseUser.getEmail()).thenReturn(TestConstants.EMAIL);
        when(mockFirebaseUser.getDisplayName()).thenReturn(TestConstants.NAME);
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(TestConstants.PHOTO_URI);
        when(mockFirebaseUser.updateProfile((UserProfileChangeRequest) Mockito.any()))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));

        when(BaseHelperShadow.sFirebaseAuth
                     .createUserWithEmailAndPassword(
                             TestConstants.EMAIL,
                             TestConstants.PASSWORD))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mockFirebaseUser),
                        true,
                        null));

        Button button = (Button) registerEmailActivity.findViewById(R.id.button_create);
        button.performClick();

        TestHelper.verifySmartLockSave(
                EmailAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                TestConstants.PASSWORD);
    }
}
