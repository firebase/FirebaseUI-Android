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
import android.os.Looper;
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
import org.robolectric.android.controller.ActivityController;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;

import androidx.test.core.app.ApplicationProvider;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
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

        ActivityController<EmailActivity> controller =
                Robolectric.buildActivity(EmailActivity.class, startIntent);
        EmailActivity activity = controller.get();
        activity.setTheme(R.style.Theme_AppCompat);
        return controller.create().start().visible().get();
    }

    private IdpResponse buildGoogleIdpResponse() {
        return new IdpResponse.Builder(
                new User.Builder(GoogleAuthProvider.PROVIDER_ID, EMAIL).build())
                .setToken(ID_TOKEN)
                .setSecret(SECRET)
                .build();
    }
}
