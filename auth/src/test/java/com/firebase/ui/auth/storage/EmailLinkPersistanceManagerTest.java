package com.firebase.ui.auth.storage;


import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.client.EmailLinkPersistenceManager;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.testhelpers.TestConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link EmailLinkPersistenceManager}.
 */
@RunWith(RobolectricTestRunner.class)
public class EmailLinkPersistanceManagerTest {


    EmailLinkPersistenceManager mPersistenceManager;

    @Before
    public void setUp() {
        mPersistenceManager = EmailLinkPersistenceManager.getInstance();
    }

    @Test
    public void testSaveAndRetrieveEmailForLink() {
        mPersistenceManager.saveEmailForLink(RuntimeEnvironment.application, TestConstants.EMAIL);

        String savedEmail = mPersistenceManager.retrieveEmailForLink(RuntimeEnvironment
                .application);
        assertThat(savedEmail).isEqualTo(TestConstants.EMAIL);
    }

    @Test
    public void testSaveAndRetrieveIdpResonseForLinking() {
        IdpResponse response = buildIdpResponse();

        mPersistenceManager.saveIdpResponseForLinking(RuntimeEnvironment.application, response);

        IdpResponse savedResponse = mPersistenceManager.retrieveIdpResponseForLinking
                (RuntimeEnvironment.application);

        assertThat(savedResponse).isEqualTo(response);
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
