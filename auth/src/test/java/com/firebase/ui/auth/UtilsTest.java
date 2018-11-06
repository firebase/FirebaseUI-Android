package com.firebase.ui.auth;

import com.firebase.ui.auth.util.data.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/** Unit tests for {@link Utils}. */
@RunWith(RobolectricTestRunner.class)
public class UtilsTest {

    @Test
    public void testGenerateRandomAlphaNumericString() {
        for (int i = 0; i < 10; i++) {
            assertThat(Utils.generateRandomAlphaNumericString(i).length()).isEqualTo(i);
        }
    }
}
