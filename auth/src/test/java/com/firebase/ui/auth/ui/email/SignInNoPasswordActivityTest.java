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
import com.firebase.ui.auth.test_helpers.FakeProviderQueryResult;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Arrays;
import java.util.Collections;


@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SignInNoPasswordActivityTest {
    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    @Test
    public void testNextButton_withInvalidEmailAddress() {
        Intent startIntent = SignInNoPasswordActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(RuntimeEnvironment.application, Arrays.asList(AuthUI
                        .EMAIL_PROVIDER)),
                null);

        SignInNoPasswordActivity noPasswordActivity =
                Robolectric.buildActivity(SignInNoPasswordActivity.class)
                        .withIntent(startIntent).create().visible().get();
        EditText email = (EditText) noPasswordActivity.findViewById(R.id.email);
        email.setText("not-a-valid-email");
        Button next = (Button) noPasswordActivity.findViewById(R.id.button_ok);
        next.performClick();

        TextInputLayout emailLayout = (TextInputLayout) noPasswordActivity
                .findViewById(R.id.input_layout_email);

        assertEquals(
                emailLayout.getError().toString(),
                noPasswordActivity.getString(R.string.invalid_email_address));
    }

    private SignInNoPasswordActivity createActivity(String email) {
        Intent startIntent = SignInNoPasswordActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        RuntimeEnvironment.application,
                        Arrays.asList(AuthUI.EMAIL_PROVIDER)),
                email);
        return Robolectric.buildActivity(SignInNoPasswordActivity.class)
                        .withIntent(startIntent).create().visible().get();
    }

    @Test
    @Config(shadows = {ActivityHelperShadow.class})
    public void testNextButton_withNewEmail() {
        SignInNoPasswordActivity noPasswordActivity = createActivity(TestConstants.EMAIL);
        Button next = (Button) noPasswordActivity.findViewById(R.id.button_ok);

        when(ActivityHelperShadow.firebaseAuth.fetchProvidersForEmail(TestConstants.EMAIL))
                .thenReturn(
                        new AutoCompleteTask<ProviderQueryResult>(
                                new FakeProviderQueryResult(Collections.<String>emptyList()),
                                true,
                                null));

        next.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(noPasswordActivity);
        ShadowActivity.IntentForResult nextIntent =
                shadowActivity.getNextStartedActivityForResult();
        assertEquals(
                RegisterEmailActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
        assertEquals(
                TestConstants.EMAIL,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
    }

    @Test
    @Config(shadows = {ActivityHelperShadow.class})
    public void testNextButton_withExistingPasswordAccount() {
        SignInNoPasswordActivity noPasswordActivity = createActivity(TestConstants.EMAIL);
        Button next = (Button) noPasswordActivity.findViewById(R.id.button_ok);

        when(ActivityHelperShadow.firebaseAuth.fetchProvidersForEmail(TestConstants.EMAIL))
                .thenReturn(
                        new AutoCompleteTask<ProviderQueryResult>(
                                new FakeProviderQueryResult(
                                        Arrays.asList(EmailAuthProvider.PROVIDER_ID)),
                                true,
                                null));

        next.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(noPasswordActivity);
        ShadowActivity.IntentForResult nextIntent =
                shadowActivity.getNextStartedActivityForResult();
        assertEquals(
                nextIntent.intent.getComponent().getClassName(),
                SignInActivity.class.getName());
        assertEquals(
                TestConstants.EMAIL,
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_EMAIL));
    }
}
