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

import static junit.framework.Assert.assertEquals;

import android.content.Intent;

import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class AuthUITest {
    private FirebaseApp mFirebaseApp;

    @Before
    public void setUp() {
        mFirebaseApp = TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    @Test
    public void testCreatingStartIntent() {
        Intent startIntent = AuthUI.getInstance(mFirebaseApp).createSignInIntentBuilder()
                .setProviders(AuthUI.EMAIL_PROVIDER, AuthUI.GOOGLE_PROVIDER)
                .setTosUrl(TestConstants.TOS_URL)
                .build();

        FlowParameters flowParameters = startIntent.getParcelableExtra(
                ExtraConstants.EXTRA_FLOW_PARAMS);

        assertEquals(flowParameters.providerInfo.size(), 2);
        assertEquals(flowParameters.appName, mFirebaseApp.getName());
        assertEquals(flowParameters.termsOfServiceUrl, TestConstants.TOS_URL);
        assertEquals(flowParameters.themeId, AuthUI.getDefaultTheme());
    }
}
