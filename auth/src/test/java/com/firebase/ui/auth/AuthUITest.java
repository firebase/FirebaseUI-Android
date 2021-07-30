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
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.EmailAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AuthUITest {
    private static final String URL = "url";
    private AuthUI mAuthUi;

    @Before
    public void setUp() {
        TestHelper.initialize();
        mAuthUi = AuthUI.getInstance(TestHelper.MOCK_APP);
    }

    @Test
    public void testCreateStartIntent_shouldHaveEmailAsDefaultProvider() {
        FlowParameters flowParameters = mAuthUi
                .createSignInIntentBuilder()
                .build()
                .getParcelableExtra(ExtraConstants.FLOW_PARAMS);
        assertEquals(1, flowParameters.providers.size());
        assertEquals(EmailAuthProvider.PROVIDER_ID,
                flowParameters.providers.get(0).getProviderId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStartIntent_shouldOnlyAllowOneInstanceOfAnIdp() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(
                new IdpConfig.EmailBuilder().build(),
                new IdpConfig.EmailBuilder().build()));
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateStartIntent_defaultProviderMustBeAvailable() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(
                new IdpConfig.EmailBuilder().build(),
                new IdpConfig.GoogleBuilder().build()))
                .setDefaultProvider(new IdpConfig.FacebookBuilder().build());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateStartIntent_incompatibleOptions() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(
                new IdpConfig.EmailBuilder().build(),
                new IdpConfig.GoogleBuilder().build()))
                .setDefaultProvider(new IdpConfig.GoogleBuilder().build())
                .setAlwaysShowSignInMethodScreen(true);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateStartIntent_incompatibleOptionsReverseOrder() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(
                new IdpConfig.EmailBuilder().build(),
                new IdpConfig.GoogleBuilder().build()))
                .setAlwaysShowSignInMethodScreen(true)
                .setDefaultProvider(new IdpConfig.GoogleBuilder().build());
    }

    @Test
    public void testCreatingStartIntent() {
        FlowParameters flowParameters = mAuthUi
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new IdpConfig.EmailBuilder().build(),
                        new IdpConfig.GoogleBuilder().build(),
                        new IdpConfig.FacebookBuilder().build(),
                        new IdpConfig.AnonymousBuilder().build()))
                .setTosAndPrivacyPolicyUrls(TestConstants.TOS_URL, TestConstants.PRIVACY_URL)
                .build()
                .getParcelableExtra(ExtraConstants.FLOW_PARAMS);

        assertEquals(4, flowParameters.providers.size());
        assertEquals(TestHelper.MOCK_APP.getName(), flowParameters.appName);
        assertEquals(TestConstants.TOS_URL, flowParameters.termsOfServiceUrl);
        assertEquals(TestConstants.PRIVACY_URL, flowParameters.privacyPolicyUrl);
        assertEquals(AuthUI.getDefaultTheme(), flowParameters.themeId);
        assertTrue(flowParameters.shouldShowProviderChoice());
        assertEquals(new IdpConfig.EmailBuilder().build(),
                flowParameters.getDefaultOrFirstProvider());
    }

    @Test
    public void testCreatingStartIntentWithDefaultProvider() {
        FlowParameters flowParameters = mAuthUi
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new IdpConfig.EmailBuilder().build(),
                        new IdpConfig.GoogleBuilder().build(),
                        new IdpConfig.FacebookBuilder().build()))
                .setDefaultProvider(new IdpConfig.FacebookBuilder().build())
                .build()
                .getParcelableExtra(ExtraConstants.FLOW_PARAMS);
        assertEquals(new IdpConfig.FacebookBuilder().build(), flowParameters.defaultProvider);
        assertFalse(flowParameters.shouldShowProviderChoice());
        assertEquals(new IdpConfig.FacebookBuilder().build(),
                flowParameters.getDefaultOrFirstProvider());
    }

    @Test(expected = NullPointerException.class)
    public void testCreatingStartIntent_withNullTos_expectEnforcesNonNullTosUrl() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setTosAndPrivacyPolicyUrls(null, TestConstants.PRIVACY_URL);
    }

    @Test(expected = NullPointerException.class)
    public void testCreatingStartIntent_withNullPp_expectEnforcesNonNullPpUrl() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setTosAndPrivacyPolicyUrls(TestConstants.TOS_URL, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreatingStartIntent_withOnlyAnonymousProvider_expectIllegalStateException() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(new IdpConfig.AnonymousBuilder().build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPhoneBuilder_withBlockedDefaultNumberCode_expectIllegalArgumentException() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("+1123456789")
                .setBlockedCountries(Arrays.asList("+1"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPhoneBuilder_withBlockedDefaultIso_expectIllegalArgumentException() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("us", "123456789")
                .setBlockedCountries(Arrays.asList("us"))
                .build();
    }

    @Test
    public void testPhoneBuilder_withAllowedDefaultIso_expectSuccess() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("us", "123456789")
                .setAllowedCountries(Arrays.asList("us"))
                .build();
    }

    @Test
    public void testPhoneBuilder_withAllowedDefaultNumberCode_expectSuccess() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("+1123456789")
                .setAllowedCountries(Arrays.asList("+1"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPhoneBuilder_withInvalidDefaultNumberCode_expectIllegalArgumentException() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("+1123456789")
                .setAllowedCountries(Arrays.asList("gr"))
                .build();
    }

    @Test
    public void testPhoneBuilder_withValidDefaultNumberCode_expectSuccess() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("+1123456789")
                .setAllowedCountries(Arrays.asList("ca"))
                .build();
    }

    @Test
    public void testPhoneBuilder_withBlockedCountryWithSameCountryCode_expectSuccess() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("+1123456789")
                .setBlockedCountries(Arrays.asList("ca"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPhoneBuilder_withInvalidDefaultIso_expectIllegalArgumentException() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("us", "123456789")
                .setAllowedCountries(Arrays.asList("ca"))
                .build();
    }

    @Test
    public void testPhoneBuilder_withValidDefaultIso_expectSucess() {
        new IdpConfig.PhoneBuilder()
                .setDefaultNumber("us", "123456789")
                .setBlockedCountries(Arrays.asList("ca"))
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void
    testPhoneBuilder_setBothBlockedAndAllowedCountries_expectIllegalStateException() {
        List<String> countries = Arrays.asList("ca");
        new IdpConfig.PhoneBuilder()
                .setBlockedCountries(countries)
                .setAllowedCountries(countries)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void
    testPhoneBuilder_passEmptyListForAllowedCountries_expectIllegalArgumentException() {
        new IdpConfig.PhoneBuilder()
                .setAllowedCountries(new ArrayList<String>())
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testPhoneBuilder_passNullForAllowedCountries_expectNullPointerException() {
        new IdpConfig.PhoneBuilder()
                .setAllowedCountries(null)
                .build();
    }


    @Test(expected = IllegalArgumentException.class)
    public void
    testPhoneBuilder_passEmptyListForBlockedCountries_expectIllegalArgumentException() {
        new IdpConfig.PhoneBuilder()
                .setBlockedCountries(new ArrayList<String>())
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testPhoneBuilder_passNullForBlockedCountries_expectNullPointerException() {
        new IdpConfig.PhoneBuilder()
                .setBlockedCountries(null)
                .build();
    }

    @Test
    public void testAnonymousBuilder_expectSuccess() {
        new IdpConfig.AnonymousBuilder()
                .build();
    }

    @Test
    public void testCustomAuthMethodPickerLayout() {
        //Testing with some random layout res
        AuthMethodPickerLayout customLayout =
                new AuthMethodPickerLayout.Builder(R.layout.fui_phone_layout)
                        .setAnonymousButtonId(123)
                        .build();

        FlowParameters flowParameters = mAuthUi
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(customLayout)
                .build()
                .getParcelableExtra(ExtraConstants.FLOW_PARAMS);

        assert flowParameters.authMethodPickerLayout != null;
        assertEquals(customLayout.getMainLayout(), flowParameters.authMethodPickerLayout.getMainLayout());
    }

    @Test
    public void testEmailBuilder_withValidActionCodeSettings_expectSuccess() {
        ActionCodeSettings actionCodeSettings =
                ActionCodeSettings.newBuilder()
                        .setUrl(URL)
                        .setHandleCodeInApp(true)
                        .build();

        IdpConfig config = new IdpConfig.EmailBuilder()
                .enableEmailLinkSignIn()
                .setActionCodeSettings(actionCodeSettings)
                .setForceSameDevice()
                .build();

        assertEquals(
                config.getParams().getParcelable(ExtraConstants.ACTION_CODE_SETTINGS),
                actionCodeSettings);
        assertTrue(config.getParams().getBoolean(ExtraConstants.FORCE_SAME_DEVICE));
        assertEquals(config.getProviderId(), AuthUI.EMAIL_LINK_PROVIDER);

    }

    @Test(expected = NullPointerException.class)
    public void testEmailBuilder_withoutActionCodeSettings_expectThrows() {
        new IdpConfig.EmailBuilder().enableEmailLinkSignIn().build();
    }

    @Test(expected = IllegalStateException.class)
    public void
    testEmailBuilder_withActionCodeSettingsAndHandleCodeInAppFalse_expectThrows() {
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder().setUrl(URL).build();
        new IdpConfig.EmailBuilder().enableEmailLinkSignIn().setActionCodeSettings
                (actionCodeSettings).build();
    }

    @Test(expected = IllegalStateException.class)
    public void testEmailBuilder_withAnonymousUpgradeAndNotForcingSameDevice_expectThrows() {
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder().setUrl(URL).build();
        new IdpConfig.EmailBuilder().enableEmailLinkSignIn().setActionCodeSettings
                (actionCodeSettings).build();
    }

    @Test
    public void testEmailBuilder_withSetDefaultEmail_expectSuccess() {
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder().setUrl(URL).build();

        IdpConfig config = new IdpConfig.EmailBuilder()
                .setDefaultEmail(TestConstants.EMAIL)
                .setActionCodeSettings(actionCodeSettings)
                .build();

        assertThat(config.getParams().getString(ExtraConstants.DEFAULT_EMAIL))
                .isEqualTo(TestConstants.EMAIL);
    }

    @Test(expected = IllegalStateException.class)
    public void testSignInIntentBuilder_anonymousUpgradeWithEmailLinkCrossDevice_expectThrows() {
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder().setUrl(URL).build();
        IdpConfig config = new IdpConfig.EmailBuilder().enableEmailLinkSignIn()
                .setActionCodeSettings(actionCodeSettings).build();

        AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(config))
                .enableAnonymousUsersAutoUpgrade();
    }
}
