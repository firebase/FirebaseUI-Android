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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.testhelpers.AuthHelperShadow;
import com.firebase.ui.auth.testhelpers.AutoCompleteTask;
import com.firebase.ui.auth.testhelpers.FakeAuthResult;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class RegisterEmailActivityTest {

    private RegisterEmailActivity createActivity() {
        Intent startIntent = RegisterEmailActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(Collections.singletonList(AuthUI.EMAIL_PROVIDER)));

        return Robolectric.buildActivity(RegisterEmailActivity.class, startIntent)
                .create()
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
        registerEmailActivity.onNewUser(
                new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build());

        Button button = registerEmailActivity.findViewById(R.id.button_create);
        button.performClick();

        TextInputLayout nameLayout = registerEmailActivity.findViewById(R.id.name_layout);
        TextInputLayout passwordLayout = registerEmailActivity.findViewById(R.id.password_layout);

        assertEquals(
                registerEmailActivity.getString(R.string.fui_required_field),
                nameLayout.getError().toString());
        assertEquals(
                String.format(
                        registerEmailActivity.getResources().getQuantityString(
                                R.plurals.fui_error_weak_password,
                                R.integer.fui_min_password_length),
                        registerEmailActivity.getResources()
                                .getInteger(R.integer.fui_min_password_length)
                ),
                passwordLayout.getError().toString());
    }

    @Test
    @Config(shadows = {AuthHelperShadow.class})
    public void testSignUpButton_successfulRegistrationShouldContinueToSaveCredentials() {
        // init mocks
        reset(AuthHelperShadow.getSaveSmartLockInstance(null));

        TestHelper.initializeApp(RuntimeEnvironment.application);
        RegisterEmailActivity registerEmailActivity = createActivity();

        // Trigger new user UI (bypassing check email)
        registerEmailActivity.onNewUser(
                new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL)
                        .setName(TestConstants.NAME)
                        .setPhotoUri(TestConstants.PHOTO_URI)
                        .build());

        EditText email = registerEmailActivity.findViewById(R.id.email);
        EditText name = registerEmailActivity.findViewById(R.id.name);
        EditText password = registerEmailActivity.findViewById(R.id.password);

        email.setText(TestConstants.EMAIL);
        name.setText(TestConstants.NAME);
        password.setText(TestConstants.PASSWORD);

        AuthHelperShadow.sCanLinkAccounts = true;
        when(AuthHelperShadow.getCurrentUser().linkWithCredential(any(EmailAuthCredential.class)))
                .thenReturn(new AutoCompleteTask<>(FakeAuthResult.INSTANCE, true, null));
        when(AuthHelperShadow.getCurrentUser().updateProfile(any(UserProfileChangeRequest.class)))
                .thenReturn(new AutoCompleteTask<Void>(null, true, null));

        Button button = registerEmailActivity.findViewById(R.id.button_create);
        button.performClick();

        // Verify create user request
        verify(AuthHelperShadow.getCurrentUser())
                .linkWithCredential(any(EmailAuthCredential.class));

        // Finally, the new credential should be saved to SmartLock
        TestHelper.verifySmartLockSave(
                EmailAuthProvider.PROVIDER_ID,
                TestConstants.EMAIL,
                TestConstants.PASSWORD,
                null);
    }
}
