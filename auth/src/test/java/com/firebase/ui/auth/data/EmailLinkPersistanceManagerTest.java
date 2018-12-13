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
import org.robolectric.RuntimeEnvironment;

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
        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);

        SessionRecord sessionRecord = mPersistenceManager
                .retrieveSessionRecord(RuntimeEnvironment.application);

        assertThat(sessionRecord.getEmail()).isEqualTo(TestConstants.EMAIL);
        assertThat(sessionRecord.getSessionId()).isEqualTo(TestConstants.SESSION_ID);
        assertThat(sessionRecord.getAnonymousUserId()).isEqualTo(TestConstants.UID);
    }

    @Test
    public void testSaveAndRetrieveIdpResonseForLinking_saveEmailFirst() {
        IdpResponse response = buildIdpResponse();

        mPersistenceManager.saveEmail(RuntimeEnvironment.application, TestConstants.EMAIL,
                TestConstants.SESSION_ID, TestConstants.UID);
        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application, response);

        SessionRecord sessionRecord = mPersistenceManager
                .retrieveSessionRecord(RuntimeEnvironment.application);

        assertThat(sessionRecord.getEmail()).isEqualTo(TestConstants.EMAIL);
        assertThat(sessionRecord.getSessionId()).isEqualTo(TestConstants.SESSION_ID);
        assertThat(sessionRecord.getAnonymousUserId()).isEqualTo(TestConstants.UID);
        assertThat(sessionRecord.getIdpResponseForLinking()).isEqualTo(response);
    }

    @Test
    public void testSaveAndRetrieveIdpResonseForLinking_noSavedEmail_expectNothingSaved() {
        IdpResponse response = buildIdpResponse();

        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application, response);

        SessionRecord sessionRecord = mPersistenceManager
                .retrieveSessionRecord(RuntimeEnvironment.application);

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
