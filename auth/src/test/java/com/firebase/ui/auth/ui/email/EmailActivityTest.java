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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;

import androidx.test.core.app.ApplicationProvider;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class EmailActivityTest {

    private static final String EMAIL = "email";
    private static final String ID_TOKEN = "idToken";
    private static final String SECRET = "secret";

    @Before
    public void setUp() {
        TestHelper.initialize();
    }

    @Test
    public void testOnCreate_passwordNormalFlow_expectCheckEmailFlowStarted() {
        EmailActivity emailActivity = createActivity(EmailAuthProvider.PROVIDER_ID);
        emailActivity.getSupportFragmentManager().findFragmentByTag(CheckEmailFragment.TAG);
    }

    @Test
    public void testOnCreate_emailLinkNormalFlow_expectCheckEmailFlowStarted() {
        EmailActivity emailActivity = createActivity(AuthUI.EMAIL_LINK_PROVIDER);
        emailActivity.getSupportFragmentManager().findFragmentByTag(CheckEmailFragment.TAG);
    }

    @Test
    public void testOnCreate_emailLinkLinkingFlow_expectSendEmailLinkFlowStarted() {
        // This is normally done by EmailLinkSendEmailHandler, saving the IdpResponse is done
        // in EmailActivity but it will not be saved if we haven't previously set the email
        EmailLinkPersistenceManager.getInstance().saveEmail(ApplicationProvider.getApplicationContext(),
                EMAIL, TestConstants.SESSION_ID, TestConstants.UID);

        EmailActivity emailActivity = createActivity(AuthUI.EMAIL_LINK_PROVIDER, true, false);

        EmailLinkFragment fragment = (EmailLinkFragment) emailActivity
                .getSupportFragmentManager().findFragmentByTag(EmailLinkFragment.TAG);
        assertThat(fragment).isNotNull();

        EmailLinkPersistenceManager persistenceManager = EmailLinkPersistenceManager.getInstance();
        IdpResponse response = persistenceManager.retrieveSessionRecord(
                ApplicationProvider.getApplicationContext()).getIdpResponseForLinking();

        assertThat(response.getProviderType()).isEqualTo(GoogleAuthProvider.PROVIDER_ID);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getIdpToken()).isEqualTo(ID_TOKEN);
        assertThat(response.getIdpSecret()).isEqualTo(SECRET);
    }

    // @Test TODO(lsirac): uncomment after figuring out why this no longer works
    public void testOnTroubleSigningIn_expectTroubleSigningInFragment() {
        EmailActivity emailActivity = createActivity(AuthUI.EMAIL_LINK_PROVIDER);

        emailActivity.onTroubleSigningIn(EMAIL);

        TroubleSigningInFragment fragment = (TroubleSigningInFragment) emailActivity
                .getSupportFragmentManager().findFragmentByTag(TroubleSigningInFragment.TAG);
        assertThat(fragment).isNotNull();
    }

    @Test
    public void testOnClickResendEmail_expectSendEmailLinkFlowStarted() {
        EmailActivity emailActivity = createActivity(AuthUI.EMAIL_LINK_PROVIDER);

        emailActivity.onClickResendEmail(EMAIL);

        EmailLinkFragment fragment = (EmailLinkFragment) emailActivity
                .getSupportFragmentManager().findFragmentByTag(EmailLinkFragment.TAG);
        assertThat(fragment).isNotNull();
    }


    @Test
    public void testSignUpButton_validatesFields() {

        EmailActivity emailActivity = createActivity(EmailAuthProvider.PROVIDER_ID);

        // Trigger RegisterEmailFragment (bypass check email)
        emailActivity.onNewUser(
                new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build());

        Button button = emailActivity.findViewById(R.id.button_create);
        button.performClick();

        TextInputLayout nameLayout = emailActivity.findViewById(R.id.name_layout);
        TextInputLayout passwordLayout = emailActivity.findViewById(R.id.password_layout);

        assertEquals(
                emailActivity.getString(R.string.fui_missing_first_and_last_name),
                nameLayout.getError().toString());
        assertEquals(
                String.format(
                        emailActivity.getResources().getQuantityString(
                                R.plurals.fui_error_weak_password,
                                R.integer.fui_min_password_length),
                        emailActivity.getResources()
                                .getInteger(R.integer.fui_min_password_length)
                ),
                passwordLayout.getError().toString());
    }

    @Test
    public void testSetDefaultEmail_validField() {
        EmailActivity emailActivity = createActivity(EmailAuthProvider.PROVIDER_ID, false, true);

        CheckEmailFragment fragment = (CheckEmailFragment) emailActivity
                .getSupportFragmentManager().findFragmentByTag(CheckEmailFragment.TAG);
        assertThat(fragment).isNotNull();

      TextInputEditText email = emailActivity.findViewById(R.id.email);
      assertEquals(TestConstants.EMAIL, email.getText().toString());

    }

    @Test
    public void testSetDefaultEmail_expectWelcomeBackPasswordPrompt() {
        EmailActivity emailActivity = createActivity(EmailAuthProvider.PROVIDER_ID, false, true);

        emailActivity.onExistingEmailUser(new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build());

        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(emailActivity).getNextStartedActivityForResult();
        assertEquals(WelcomeBackPasswordPrompt.class.getName(),
                nextIntent.intent.getComponent().getClassName());

    }

    @Test
    public void testSetDefaultEmail_expectRegisterEmailFragment() {
        EmailActivity emailActivity = createActivity(EmailAuthProvider.PROVIDER_ID, false, true);

        emailActivity.onNewUser(new User.Builder(EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL).build());

        RegisterEmailFragment registerEmailFragment = (RegisterEmailFragment) emailActivity
                .getSupportFragmentManager().findFragmentByTag(RegisterEmailFragment.TAG);
        assertThat(registerEmailFragment).isNotNull();
    }


    private EmailActivity createActivity(String providerId) {
        return createActivity(providerId, false, false);
    }

    private EmailActivity createActivity(String providerId, boolean emailLinkLinkingFlow, boolean hasDefaultEmail) {
        Intent startIntent = EmailActivity.createIntent(
                ApplicationProvider.getApplicationContext(),
                TestHelper.getFlowParameters(Collections.singletonList(providerId)));

        if (hasDefaultEmail) {
             startIntent = EmailActivity.createIntent(
                    ApplicationProvider.getApplicationContext(),
                    TestHelper.getFlowParameters(Collections.singletonList(providerId), false, null, true));
        }

        if (emailLinkLinkingFlow) {
            startIntent.putExtra(ExtraConstants.EMAIL, EMAIL);
            startIntent.putExtra(ExtraConstants.IDP_RESPONSE, buildGoogleIdpResponse());
        }

        return Robolectric.buildActivity(EmailActivity.class, startIntent)
                .create()
                .start()
                .visible()
                .get();
    }

    private IdpResponse buildGoogleIdpResponse() {
        return new IdpResponse.Builder(
                new User.Builder(GoogleAuthProvider.PROVIDER_ID, EMAIL).build())
                .setToken(ID_TOKEN)
                .setSecret(SECRET)
                .build();
    }
}
