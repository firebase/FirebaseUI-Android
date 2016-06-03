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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FacebookProviderShadow;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.test_helpers.GoogleProviderShadow;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.account_link.SaveCredentialsActivity;
import com.firebase.ui.auth.ui.email.EmailHintContainerActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Arrays;
import java.util.List;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {
                FirebaseAuthWrapperImplShadow.class,
                GoogleProviderShadow.class,
                FacebookProviderShadow.class
        }, sdk = 21)
public class AuthMethodPickerActivityTest {

    @Before
    public void setUp() {}

    @Test
    public void testAllProvidersArePopulated() {
        List<String> providers = Arrays.asList(
                AuthUI.FACEBOOK_PROVIDER,
                AuthUI.GOOGLE_PROVIDER,
                AuthUI.EMAIL_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity =
                createActivity(providers);
        assertEquals(providers.size(),
                ((LinearLayout) authMethodPickerActivity.findViewById(R.id.btn_holder))
                        .getChildCount());
        Button emailButton = (Button) authMethodPickerActivity.findViewById(R.id.email_provider);
        assertEquals(View.VISIBLE, emailButton.getVisibility());
    }


    @Test
    public void testEmailIsHidden() {
        List<String> providers = Arrays.asList(
                AuthUI.FACEBOOK_PROVIDER,
                AuthUI.GOOGLE_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity =
                createActivity(providers);

        assertEquals(providers.size() + 1, // plus one due to the invisible email button
                ((LinearLayout) authMethodPickerActivity.findViewById(R.id.btn_holder))
                        .getChildCount());
        Button emailButton = (Button) authMethodPickerActivity.findViewById(R.id.email_provider);
        assertEquals(View.GONE, emailButton.getVisibility());
    }

    @Test
    public void testEmailLoginFlow() {
        List<String> providers = Arrays.asList(AuthUI.EMAIL_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity =
                createActivity(providers);

        Button emailButton = (Button) authMethodPickerActivity.findViewById(R.id.email_provider);
        emailButton.performClick();
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();

        assertEquals(
                EmailHintContainerActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
    }


    @Test
    @Config(shadows = {ActivityHelperShadow.class})
    public void testFacebookLoginFlow() {
        List<String> providers = Arrays.asList(AuthUI.FACEBOOK_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity =
                createActivity(providers);

        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        when(mockFirebaseUser.getProviders())
                .thenReturn(Arrays.asList(FacebookAuthProvider.PROVIDER_ID));
        when(ActivityHelperShadow.firebaseAuth.signInWithCredential((AuthCredential) anyObject()))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mockFirebaseUser), true, null));

        Button facebookButton =
                (Button) authMethodPickerActivity.findViewById(R.id.facebook_button);
        assertNotNull(facebookButton);
        facebookButton.performClick();

        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();
        verifySaveCredentialIntent(nextIntent, FacebookAuthProvider.PROVIDER_ID);
    }

    @Test
    @Config(shadows = {GoogleProviderShadow.class, ActivityHelperShadow.class})
    public void testGoogleLoginFlow() {
        List<String> providers = Arrays.asList(AuthUI.GOOGLE_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity =
                createActivity(providers);

        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        when(mockFirebaseUser.getProviders())
                .thenReturn(Arrays.asList(GoogleAuthProvider.PROVIDER_ID));

        when(ActivityHelperShadow.firebaseAuth.signInWithCredential((AuthCredential) anyObject()))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mockFirebaseUser), true, null));
        Button googleButton =
                (Button) authMethodPickerActivity.findViewById(R.id.google_button);

        assertNotNull(googleButton);
        googleButton.performClick();
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();

        verifySaveCredentialIntent(nextIntent, GoogleAuthProvider.PROVIDER_ID);
    }

    private static void verifySaveCredentialIntent(
            ShadowActivity.IntentForResult nextIntent,
            String provider) {
        assertEquals(
                SaveCredentialsActivity.class.getName(),
                nextIntent.intent.getComponent().getClassName());
        assertEquals(
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_EMAIL),
                TestConstants.EMAIL);
        assertEquals(
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_NAME),
                TestConstants.NAME);
        assertEquals(
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_PROFILE_PICTURE_URI),
                TestConstants.PHOTO_URL);
        assertEquals(
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_PROVIDER),
                provider);
        assertEquals(
                nextIntent.intent.getExtras().getString(ExtraConstants.EXTRA_PASSWORD),
                null);
    }

    private AuthMethodPickerActivity createActivity(List<String> providers) {
        Intent startIntent = AuthMethodPickerActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        RuntimeEnvironment.application,
                        providers));

        return Robolectric
                .buildActivity(AuthMethodPickerActivity.class)
                .withIntent(startIntent)
                .create()
                .visible()
                .get();
    }
}
