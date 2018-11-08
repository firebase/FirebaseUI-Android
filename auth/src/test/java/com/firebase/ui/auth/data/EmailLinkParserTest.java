package com.firebase.ui.auth.data;

import com.firebase.ui.auth.util.data.EmailLinkParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link EmailLinkParser}.
 */
@RunWith(RobolectricTestRunner.class)
public class EmailLinkParserTest {

    private static final String SESSION_ID = "sessionId";
    private static final String ANONYMOUS_USER_ID = "anonymousUserId";
    private static final String PROVIDER_ID = "providerId";
    private static final boolean FORCE_SAME_DEVICE = true;

    private static final String CONTINUE_URL = "https://google.com" +
            "?ui_sid=" + SESSION_ID + "&ui_auid=" + ANONYMOUS_USER_ID + "&ui_pid=" + PROVIDER_ID
            + "&ui_sd=" + (FORCE_SAME_DEVICE ? "1" : "0");

    private static final String OOB_CODE = "anOobCode";
    private static final String ENCODED_EMAIL_LINK =
            "https://www.fake.com?link=https://fake.firebaseapp.com/__/auth/action?"
                    + "%26mode%3DsignIn%26oobCode%3D" + OOB_CODE
                    + "%26continueUrl%3D" + CONTINUE_URL;

    private static final String DECODED_EMAIL_LINK =
            "https://fake.com/__/auth/action?apiKey=apiKey&mode=signIn"
                    + "&oobCode=" + OOB_CODE
                    + "&continueUrl=" + CONTINUE_URL;


    private static final String MALFORMED_LINK = "not_a_hierarchical_link:";

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_validStringWithNoParams_expectThrows() {
        new EmailLinkParser(MALFORMED_LINK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullLink_expectThrows() {
        new EmailLinkParser(/*link=*/null);
    }

    @Test
    public void testGetters_encodedLink() {
        EmailLinkParser parser = new EmailLinkParser(ENCODED_EMAIL_LINK);
        assertThat(parser.getOobCode()).isEqualTo(OOB_CODE);
        assertThat(parser.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(parser.getAnonymousUserId()).isEqualTo(ANONYMOUS_USER_ID);
        assertThat(parser.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(parser.getForceSameDeviceBit()).isEqualTo(FORCE_SAME_DEVICE);
    }

    @Test
    public void testGetters_decodedLink() {
        EmailLinkParser parser = new EmailLinkParser(DECODED_EMAIL_LINK);
        assertThat(parser.getOobCode()).isEqualTo(OOB_CODE);
        assertThat(parser.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(parser.getAnonymousUserId()).isEqualTo(ANONYMOUS_USER_ID);
        assertThat(parser.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(parser.getForceSameDeviceBit()).isEqualTo(FORCE_SAME_DEVICE);
    }

    @Test
    public void testGetters_noContinueUrlParams() {
        String encodedLink = ENCODED_EMAIL_LINK.substring(0,
                ENCODED_EMAIL_LINK.length() - CONTINUE_URL.length());
        EmailLinkParser parser = new EmailLinkParser(encodedLink);
        assertThat(parser.getOobCode()).isEqualTo(OOB_CODE);
        assertThat(parser.getSessionId()).isNull();
        assertThat(parser.getAnonymousUserId()).isNull();
        assertThat(parser.getProviderId()).isNull();
        assertThat(parser.getForceSameDeviceBit()).isFalse();
    }

}
