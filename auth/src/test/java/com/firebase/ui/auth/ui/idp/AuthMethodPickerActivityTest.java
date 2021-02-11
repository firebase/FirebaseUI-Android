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

package com.firebase.ui.auth.ui.idp;

import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.Arrays;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class AuthMethodPickerActivityTest {
    @Before
    public void setUp() {
        TestHelper.initialize();
    }

    @Test
    public void testAllProvidersArePopulated() {
        // Exclude Facebook until the `NoClassDefFoundError: com/facebook/common/R$style` exception
        // is fixed.
        List<String> providers = Arrays.asList(
                GoogleAuthProvider.PROVIDER_ID,
                TwitterAuthProvider.PROVIDER_ID,
                EmailAuthProvider.PROVIDER_ID,
                PhoneAuthProvider.PROVIDER_ID,
                AuthUI.ANONYMOUS_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity = createActivity(providers);

        assertEquals(providers.size(),
                ((LinearLayout) authMethodPickerActivity.findViewById(R.id.btn_holder))
                        .getChildCount());
    }

    @Test
    public void testEmailLoginFlow() {
        List<String> providers = Arrays.asList(EmailAuthProvider.PROVIDER_ID);

        AuthMethodPickerActivity authMethodPickerActivity = createActivity(providers);

        Button emailButton = authMethodPickerActivity.findViewById(R.id.email_button);
        emailButton.performClick();
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();

        assertEquals(
                EmailActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
    }

    @Test
    public void testPhoneLoginFlow() {
        List<String> providers = Arrays.asList(PhoneAuthProvider.PROVIDER_ID);

        AuthMethodPickerActivity authMethodPickerActivity = createActivity(providers);

        Button phoneButton = authMethodPickerActivity.findViewById(R.id.phone_button);
        phoneButton.performClick();
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();

        assertEquals(
                PhoneActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
    }

    @Test
    public void testCustomAuthMethodPickerLayout() {
        List<String> providers = Arrays.asList(EmailAuthProvider.PROVIDER_ID);

        AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout
                .Builder(R.layout.fui_provider_button_email)
                .setEmailButtonId(R.id.email_button)
                .build();

        AuthMethodPickerActivity authMethodPickerActivity = createActivityWithCustomLayout(providers, customLayout, false);

        Button emailButton = authMethodPickerActivity.findViewById(R.id.email_button);
        emailButton.performClick();

        //Expected result -> Directing users to EmailActivity
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();
        assertEquals(
                EmailActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
    }

    @Test
    public void testCustomAuthMethodPickerLayoutWithEmailLink() {
        List<String> providers = Arrays.asList(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD);

        AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout
                .Builder(R.layout.fui_provider_button_email)
                .setEmailButtonId(R.id.email_button)
                .build();

        AuthMethodPickerActivity authMethodPickerActivity = createActivityWithCustomLayout(providers, customLayout, false);

        Button emailButton = authMethodPickerActivity.findViewById(R.id.email_button);
        emailButton.performClick();

        //Expected result -> Directing users to EmailActivity
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();
        assertEquals(
                EmailActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
    }

    @Test
    public void testCustomAuthMethodPickerLayoutWithDefaultEmail() {
        List<String> providers = Arrays.asList(EmailAuthProvider.PROVIDER_ID);

        AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout
                .Builder(R.layout.fui_provider_button_email)
                .setEmailButtonId(R.id.email_button)
                .build();

        AuthMethodPickerActivity authMethodPickerActivity = createActivityWithCustomLayout(providers, customLayout, true);
        Button emailButton = authMethodPickerActivity.findViewById(R.id.email_button);
        emailButton.performClick();

        //Expected result -> Directing users to EmailActivity
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();
        assertEquals(
                EmailActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
    }

    private AuthMethodPickerActivity createActivityWithCustomLayout(List<String> providers,
                                                                    AuthMethodPickerLayout layout,
                                                                    boolean hasDefaultEmail) {
        Intent startIntent = AuthMethodPickerActivity.createIntent(
                ApplicationProvider.getApplicationContext(),
                TestHelper.getFlowParameters(providers, false, layout, hasDefaultEmail));

        return Robolectric
                .buildActivity(AuthMethodPickerActivity.class, startIntent)
                .create()
                .visible()
                .get();
    }

    private AuthMethodPickerActivity createActivity(List<String> providers) {
        Intent startIntent = AuthMethodPickerActivity.createIntent(
                ApplicationProvider.getApplicationContext(),
                TestHelper.getFlowParameters(providers));

        return Robolectric
                .buildActivity(AuthMethodPickerActivity.class, startIntent)
                .create()
                .visible()
                .get();
    }
}
