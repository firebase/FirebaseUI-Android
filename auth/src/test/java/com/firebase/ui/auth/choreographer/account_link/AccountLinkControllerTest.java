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

package com.firebase.ui.auth.choreographer.account_link;

import android.app.Activity;
import android.content.Intent;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.api.ShadowFirebaseAuthWrapperFactory;
import com.firebase.ui.auth.api.FirebaseAuthWrapper;
import com.firebase.ui.auth.choreographer.Action;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.Result;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class AccountLinkControllerTest {
    private AccountLinkController mAccountLinkController;

    private static final String EMAIL = "test@example.com";
    private static final String APP_NAME = "My App";

    @Mock private FirebaseAuthWrapper mMockHeadlessAPIWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAccountLinkController = new AccountLinkController(RuntimeEnvironment.application, APP_NAME);
        ShadowFirebaseAuthWrapperFactory.setFirebaseAuthWrapper(mMockHeadlessAPIWrapper);
    }

    @Test
    @Config(shadows={ShadowFirebaseAuthWrapperFactory.class})
    public void testNewCredentialFlow() {
        Intent startingIntent = new Intent()
                .putExtra(ControllerConstants.EXTRA_APP_NAME, APP_NAME)
                .putExtra(ControllerConstants.EXTRA_EMAIL, EMAIL)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, EmailAuthProvider.PROVIDER_ID);

        // new account, so no providers
        when(mMockHeadlessAPIWrapper.getProvidersForEmail(EMAIL)).thenReturn(
                new ArrayList<String>());

        Result result = new Result(
                AccountLinkController.ID_INIT,
                Activity.RESULT_OK,
                startingIntent);
        Action nextAction = mAccountLinkController.next(result);
        assertEquals(nextAction.getNextId(), AccountLinkController.ID_CREDENTIALS_SAVE);
    }

    @Test
    @Config(shadows={ShadowFirebaseAuthWrapperFactory.class})
    public void testPasswordProviderMismatch() {
        Intent startingIntent = new Intent()
                .putExtra(ControllerConstants.EXTRA_APP_NAME, APP_NAME)
                .putExtra(ControllerConstants.EXTRA_EMAIL, EMAIL)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, EmailAuthProvider.PROVIDER_ID);

        // claim that there is a Google Auth account that exists
        when(mMockHeadlessAPIWrapper.getProvidersForEmail(EMAIL)).thenReturn(
                Arrays.asList(GoogleAuthProvider.PROVIDER_ID));

        Result result = new Result(
                AccountLinkController.ID_INIT,
                Activity.RESULT_OK,
                startingIntent);
        Action nextAction = mAccountLinkController.next(result);
        assertEquals(nextAction.getNextId(), AccountLinkController.ID_WELCOME_BACK_IDP);
    }

    @Test
    @Config(shadows={ShadowFirebaseAuthWrapperFactory.class})
    public void testIDPProviderMismatch() {
        Intent startingIntent = new Intent()
                .putExtra(ControllerConstants.EXTRA_APP_NAME, APP_NAME)
                .putExtra(ControllerConstants.EXTRA_EMAIL, EMAIL)
                .putExtra(ControllerConstants.EXTRA_PROVIDER, GoogleAuthProvider.PROVIDER_ID);

        // claim that there is an email/password account that exists
        when(mMockHeadlessAPIWrapper.getProvidersForEmail(EMAIL)).thenReturn(
                Arrays.asList(EmailAuthProvider.PROVIDER_ID));

        Result result = new Result(
                AccountLinkController.ID_INIT,
                Activity.RESULT_OK,
                startingIntent);
        Action nextAction = mAccountLinkController.next(result);
        assertEquals(nextAction.getNextId(), AccountLinkController.ID_WELCOME_BACK_PASSWORD);
    }
}
