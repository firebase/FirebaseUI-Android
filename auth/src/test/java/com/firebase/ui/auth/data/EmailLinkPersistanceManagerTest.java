/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.data;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager.SessionRecord;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;

import static com.google.common.truth.Truth.assertThat;

/** Unit tests for {@link EmailLinkPersistenceManager}.*/
@RunWith(RobolectricTestRunner.class)
public class EmailLinkPersistanceManagerTest {


    EmailLinkPersistenceManager mPersistenceManager;

    @Before
    public void setUp() {
        mPersistenceManager = EmailLinkPersistenceManager.getInstance();
    }

    @Test
    public void testSaveAndRetrieveEmailForLink() {
        mPersistenceManager.saveEmail(
                ApplicationProvider.getApplicationContext(),
                TestConstants.EMAIL, TestConstants.SESSION_ID, TestConstants.UID);

        SessionRecord sessionRecord = mPersistenceManager
                .retrieveSessionRecord(ApplicationProvider.getApplicationContext());

        assertThat(sessionRecord.getEmail()).isEqualTo(TestConstants.EMAIL);
        assertThat(sessionRecord.getSessionId()).isEqualTo(TestConstants.SESSION_ID);
        assertThat(sessionRecord.getAnonymousUserId()).isEqualTo(TestConstants.UID);
    }

    @Test
    public void testSaveAndRetrieveIdpResonseForLinking_saveEmailFirst() {
        IdpResponse response = buildIdpResponse();

        mPersistenceManager.saveEmail(
                ApplicationProvider.getApplicationContext(),
                TestConstants.EMAIL, TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(
                ApplicationProvider.getApplicationContext(), response);

        SessionRecord sessionRecord = mPersistenceManager
                .retrieveSessionRecord(ApplicationProvider.getApplicationContext());

        assertThat(sessionRecord.getEmail()).isEqualTo(TestConstants.EMAIL);
        assertThat(sessionRecord.getSessionId()).isEqualTo(TestConstants.SESSION_ID);
        assertThat(sessionRecord.getAnonymousUserId()).isEqualTo(TestConstants.UID);
        assertThat(sessionRecord.getIdpResponseForLinking()).isEqualTo(response);
    }

    @Test
    public void testSaveAndRetrieveIdpResonseForLinking_noSavedEmail_expectNothingSaved() {
        IdpResponse response = buildIdpResponse();

        mPersistenceManager.saveIdpResponseForLinking(
                ApplicationProvider.getApplicationContext(), response);

        SessionRecord sessionRecord = mPersistenceManager
                .retrieveSessionRecord(ApplicationProvider.getApplicationContext());

        assertThat(sessionRecord).isNull();
    }

    private IdpResponse buildIdpResponse() {
        User user = new User.Builder(AuthUI.EMAIL_LINK_PROVIDER, TestConstants.EMAIL)
                .build();

        return new IdpResponse.Builder(user)
                .setToken(TestConstants.TOKEN)
                .setSecret(TestConstants.SECRET)
                .build();
    }
}
