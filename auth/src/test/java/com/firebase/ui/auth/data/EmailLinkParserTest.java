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

    private static final String OOB_CODE = "anOobCode";
    private static final String ENCODED_EMAIL_LINK =
            "https://www.fake.com?link=https://fake.firebaseapp.com/__/auth/action?"
                    + "%26mode%3DsignIn%26oobCode%3D"
                    + OOB_CODE;
    private static final String DECODED_EMAIL_LINK =
            "https://fake.com/__/auth/action?apiKey=apiKey&mode=signIn"
                    + "&oobCode="
                    + OOB_CODE;
    private static final String MALFORMED_LINK = "not_a_hierarchical_link:";

    private EmailLinkParser mEmailLinkParser;

    @Before
    public void setUp() {
        mEmailLinkParser = mEmailLinkParser.getInstance();
    }

    @Test
    public void testGetOobCodeFromLink_encodedLink() {
        String oobCode = mEmailLinkParser.getOobCodeFromLink(ENCODED_EMAIL_LINK);
        assertThat(oobCode).isEqualTo(OOB_CODE);
    }

    @Test
    public void testGetOobCodeFromLink_decodedLink() {
        String oobCode = mEmailLinkParser.getOobCodeFromLink(DECODED_EMAIL_LINK);
        assertThat(oobCode).isEqualTo(OOB_CODE);
    }

    @Test
    public void testGetOobCodeFromLink_malformedLink() {
        String oobCode = mEmailLinkParser.getOobCodeFromLink(MALFORMED_LINK);
        assertThat(oobCode).isNull();
    }
}
