package com.firebase.ui.auth.util

import androidx.annotation.RestrictTo
import kotlin.random.Random

/**
 * Utility for generating random session identifiers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SessionUtils {

    private const val VALID_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    /**
     * Generates a random alphanumeric string.
     *
     * @param length The desired length of the generated string.
     * @return A randomly generated string with the desired number of characters.
     */
    fun generateRandomAlphaNumericString(length: Int): String {
        return (1..length)
            .map { VALID_CHARS[Random.nextInt(VALID_CHARS.length)] }
            .joinToString("")
    }
}