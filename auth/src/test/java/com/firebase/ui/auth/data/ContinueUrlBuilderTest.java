package com.firebase.ui.auth.data;

import com.firebase.ui.auth.util.data.ContinueUrlBuilder;
import com.firebase.ui.auth.util.data.EmailLinkParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link ContinueUrlBuilder}.
 */
@RunWith(RobolectricTestRunner.class)
public class ContinueUrlBuilderTest {


    private static final String ENCODED_EMAIL_LINK =
            "https://www.fake.com?link=https://fake.firebaseapp.com/__/auth/action?"
                    + "%26mode%3DsignIn%26continueUrl%3Dhttps://google.com";

    private static final String DECODED_EMAIL_LINK = "https://fake.com/__/auth/action?apiKey=apiKey"
                    + "&mode=signIn&continueUrl=https://google.com";

    private static final String SESSION_ID = "sessionId";
    private static final String ANONYMOUS_USER_ID = "anonymousUserId";
    private static final String PROVIDER_ID = "providerId";
    private static final boolean FORCE_SAME_DEVICE = true;

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullUrl_expectsThrows() {
        new ContinueUrlBuilder(/*url=*/null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_emptyUrl_expectsThrows() {
        new ContinueUrlBuilder(/*url=*/null);
    }

    @Test
    public void testAppendParams_encodedLink_expectSuccess() {
        String continueUrlBuilder = new ContinueUrlBuilder(ENCODED_EMAIL_LINK)
                .appendSessionId(SESSION_ID)
                .appendAnonymousUserId(ANONYMOUS_USER_ID)
                .appendProviderId(PROVIDER_ID)
                .appendForceSameDeviceBit(FORCE_SAME_DEVICE)
                .build();

        EmailLinkParser parser = new EmailLinkParser(continueUrlBuilder);
        assertThat(parser.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(parser.getAnonymousUserId()).isEqualTo(ANONYMOUS_USER_ID);
        assertThat(parser.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(parser.getForceSameDeviceBit()).isEqualTo(FORCE_SAME_DEVICE);
    }

    @Test
    public void testAppendParams_decodedLink_expectSuccess() {
        String continueUrlBuilder = new ContinueUrlBuilder(DECODED_EMAIL_LINK)
                .appendSessionId(SESSION_ID)
                .appendAnonymousUserId(ANONYMOUS_USER_ID)
                .appendProviderId(PROVIDER_ID)
                .appendForceSameDeviceBit(FORCE_SAME_DEVICE)
                .build();

        EmailLinkParser parser = new EmailLinkParser(continueUrlBuilder);
        assertThat(parser.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(parser.getAnonymousUserId()).isEqualTo(ANONYMOUS_USER_ID);
        assertThat(parser.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(parser.getForceSameDeviceBit()).isEqualTo(FORCE_SAME_DEVICE);
    }

    @Test
    public void testAppendParams_nullValues_expectNoParamsAdded() {
        String continueUrl = new ContinueUrlBuilder(ENCODED_EMAIL_LINK)
                .appendSessionId(null)
                .appendAnonymousUserId(null)
                .appendProviderId(null)
                .build();

        assertThat(continueUrl).isEqualTo(ENCODED_EMAIL_LINK);
    }
}
