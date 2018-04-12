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

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.google.firebase.auth.EmailAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class WelcomeBackPasswordPromptTest {
    @Before
    public void setUp() {
        TestHelper.initialize();
    }

    private WelcomeBackPasswordPrompt createActivity() {
        Intent startIntent = WelcomeBackPasswordPrompt.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(Collections.singletonList(EmailAuthProvider.PROVIDER_ID)),
                new IdpResponse.Builder(new User.Builder(
                        EmailAuthProvider.PROVIDER_ID, TestConstants.EMAIL
                ).build()).build());
        return Robolectric
                .buildActivity(WelcomeBackPasswordPrompt.class, startIntent)
                .create()
                .visible()
                .get();
    }

    @Test
    public void testSignInButton_validatesFields() {
        WelcomeBackPasswordPrompt welcomeBack = createActivity();
        Button signIn = welcomeBack.findViewById(R.id.button_done);
        signIn.performClick();
        TextInputLayout passwordLayout =
                welcomeBack.findViewById(R.id.password_layout);

        assertEquals(
                welcomeBack.getString(R.string.fui_required_field),
                passwordLayout.getError().toString());

        // should block and not start a new activity
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(welcomeBack).getNextStartedActivityForResult();
        assertNull(nextIntent);
    }
}
