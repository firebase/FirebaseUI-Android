package com.firebase.ui.auth.util.data;

import java.util.Random;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SessionUtils {

    private static final String VALID_CHARS =
            "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Generates a random alpha numeric string.
     * @param length the desired length of the generated string.
     * @return a randomly generated string with the desired number of characters.
     */
    public static String generateRandomAlphaNumericString(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(VALID_CHARS.charAt(random.nextInt(length)));
        }
        return sb.toString();
    }
}
