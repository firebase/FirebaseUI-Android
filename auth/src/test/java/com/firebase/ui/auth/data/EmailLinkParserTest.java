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

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_validStringWithNoParams_expectThrows() {
        new EmailLinkParser(MALFORMED_LINK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullLink_expectThrows() {
        new EmailLinkParser(MALFORMED_LINK);
    }

    @Test
    public void testGetOobCode_encodedLink() {
        EmailLinkParser parser = new EmailLinkParser(ENCODED_EMAIL_LINK);
        String oobCode = parser.getOobCode();
        assertThat(oobCode).isEqualTo(OOB_CODE);
    }

    @Test
    public void testGetOobCode_decodedLink() {
        EmailLinkParser parser = new EmailLinkParser(DECODED_EMAIL_LINK);
        String oobCode = parser.getOobCode();
        assertThat(oobCode).isEqualTo(OOB_CODE);
    }
}
