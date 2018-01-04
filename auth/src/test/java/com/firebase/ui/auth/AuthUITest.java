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

package com.firebase.ui.auth;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.AuthUI.SignInIntentBuilder;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AuthUITest {
    @Before
    public void setUp() {
        TestHelper.initialize();
    }

    @Test
    public void testCreateStartIntent_shouldHaveEmailAsDefaultProvider() {
        FlowParameters flowParameters = AuthUI
                .getInstance()
                .createSignInIntentBuilder()
                .build()
                .getParcelableExtra(ExtraConstants.EXTRA_FLOW_PARAMS);
        assertEquals(1, flowParameters.providerInfo.size());
        assertEquals(EmailAuthProvider.PROVIDER_ID,
                flowParameters.providerInfo.get(0).getProviderId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStartIntent_shouldOnlyAllowOneInstanceOfAnIdp() {
        SignInIntentBuilder startIntent = AuthUI.getInstance().createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(
                new IdpConfig.EmailBuilder().build(),
                new IdpConfig.EmailBuilder().build()));
    }

    @Test
    public void testCreatingStartIntent() {
        FlowParameters flowParameters = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new IdpConfig.EmailBuilder().build(),
                        new IdpConfig.GoogleBuilder().build(),
                        new IdpConfig.FacebookBuilder().build()))
                .setTosUrl(TestConstants.TOS_URL)
                .setPrivacyPolicyUrl(TestConstants.PRIVACY_URL)
                .build()
                .getParcelableExtra(ExtraConstants.EXTRA_FLOW_PARAMS);

        assertEquals(3, flowParameters.providerInfo.size());
        assertEquals(FirebaseApp.getInstance().getName(), flowParameters.appName);
        assertEquals(TestConstants.TOS_URL, flowParameters.termsOfServiceUrl);
        assertEquals(TestConstants.PRIVACY_URL, flowParameters.privacyPolicyUrl);
        assertEquals(AuthUI.getDefaultTheme(), flowParameters.themeId);
    }
}
