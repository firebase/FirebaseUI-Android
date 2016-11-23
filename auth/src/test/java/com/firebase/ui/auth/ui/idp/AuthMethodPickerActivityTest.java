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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.AutoCompleteTask;
import com.firebase.ui.auth.test_helpers.BaseHelperShadow;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FacebookProviderShadow;
import com.firebase.ui.auth.test_helpers.FakeAuthResult;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.test_helpers.GoogleProviderShadow;
import com.firebase.ui.auth.test_helpers.LoginManagerShadow;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.firebase.ui.auth.ui.email.EmailHintContainerActivity;
import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

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

import static com.firebase.ui.auth.test_helpers.TestHelper.verifySmartLockSave;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {
                FirebaseAuthWrapperImplShadow.class,
                GoogleProviderShadow.class,
                FacebookProviderShadow.class,
                LoginManagerShadow.class
        }, sdk = 23)
public class AuthMethodPickerActivityTest {

    @Before
    public void setUp() {
        PlayServicesHelper.sApiAvailability = TestHelper.makeMockGoogleApiAvailability();
    }

    @Test
    public void testAllProvidersArePopulated() {
        List<String> providers = Arrays.asList(
                AuthUI.FACEBOOK_PROVIDER,
                AuthUI.GOOGLE_PROVIDER,
                AuthUI.TWITTER_PROVIDER,
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
    @Config(shadows = {BaseHelperShadow.class, ActivityHelperShadow.class})
    public void testFacebookLoginFlow() {
        // initialize mocks
        new ActivityHelperShadow();
        reset(ActivityHelperShadow.sSaveSmartLock);

        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        when(mockFirebaseUser.getProviders())
                .thenReturn(Arrays.asList(FacebookAuthProvider.PROVIDER_ID));
        when(ActivityHelperShadow.sFirebaseAuth.signInWithCredential((AuthCredential) any()))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mockFirebaseUser), true, null));

        List<String> providers = Arrays.asList(AuthUI.FACEBOOK_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity = createActivity(providers);

        Button facebookButton =
                (Button) authMethodPickerActivity.findViewById(R.id.facebook_button);
        assertNotNull(facebookButton);
        facebookButton.performClick();

        verifySmartLockSave(AuthUI.FACEBOOK_PROVIDER, TestConstants.EMAIL, null);
    }

    @Test
    @Config(shadows = {GoogleProviderShadow.class, BaseHelperShadow.class, ActivityHelperShadow.class})
    public void testGoogleLoginFlow() {
        // initialize mocks
        new ActivityHelperShadow();
        reset(ActivityHelperShadow.sSaveSmartLock);

        List<String> providers = Arrays.asList(AuthUI.GOOGLE_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity = createActivity(providers);

        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();
        when(mockFirebaseUser.getProviders())
                .thenReturn(Arrays.asList(GoogleAuthProvider.PROVIDER_ID));

        when(ActivityHelperShadow.sFirebaseAuth.signInWithCredential((AuthCredential) any()))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mockFirebaseUser), true, null));

        Button googleButton =
                (Button) authMethodPickerActivity.findViewById(R.id.google_button);

        assertNotNull(googleButton);
        googleButton.performClick();

        verifySmartLockSave(AuthUI.GOOGLE_PROVIDER, TestConstants.EMAIL, null);
    }



    @Test
    @Config(shadows = {ActivityHelperShadow.class})
    public void testTwitterLoginFlowStarts() {
        List<String> providers = Arrays.asList(AuthUI.TWITTER_PROVIDER);

        AuthMethodPickerActivity authMethodPickerActivity = createActivity(providers);

        FirebaseUser mockFirebaseUser = TestHelper.makeMockFirebaseUser();

        when(mockFirebaseUser.getProviders())
                .thenReturn(Arrays.asList(TwitterAuthProvider.PROVIDER_ID));

        when(ActivityHelperShadow.sFirebaseAuth.signInWithCredential((AuthCredential) any()))
                .thenReturn(new AutoCompleteTask<AuthResult>(
                        new FakeAuthResult(mockFirebaseUser), true, null));
        Button twitterButton =
                (Button) authMethodPickerActivity.findViewById(R.id.twitter_button);

        assertNotNull(twitterButton);
        twitterButton.performClick();
        ShadowActivity.IntentForResult nextIntent =
                Shadows.shadowOf(authMethodPickerActivity).getNextStartedActivityForResult();

        assertTrue(nextIntent.intent.getComponent().getClassName().contains("com.twitter.sdk"));
    }

    private AuthMethodPickerActivity createActivity(List<String> providers) {
        Intent startIntent = AuthMethodPickerActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(providers));

        return Robolectric
                .buildActivity(AuthMethodPickerActivity.class)
                .withIntent(startIntent)
                .create()
                .visible()
                .get();
    }
}
