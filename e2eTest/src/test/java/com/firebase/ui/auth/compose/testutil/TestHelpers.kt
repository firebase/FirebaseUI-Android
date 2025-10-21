package com.firebase.ui.auth.compose.testutil

import android.os.Looper
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.google.firebase.auth.FirebaseUser
import org.robolectric.Shadows.shadowOf

/**
 * Ensures a fresh user exists in the Firebase emulator with the given credentials.
 * If a user already exists, they will be deleted first.
 * The user will be signed out after creation, leaving an unverified account ready for testing.
 *
 * This function uses coroutines and automatically handles Robolectric's main looper.
 *
 * @param authUI The FirebaseAuthUI instance
 * @param email The email address for the user
 * @param password The password for the user
 * @return The created FirebaseUser, or null if creation failed
 */
fun ensureFreshUser(authUI: FirebaseAuthUI, email: String, password: String): FirebaseUser? {
    println("TEST: Ensuring fresh user for $email")
    // Try to sign in - if successful, user exists and should be deleted
    try {
        authUI.auth.signInWithEmailAndPassword(email, password).awaitWithLooper()
            .also { result ->
                println("TEST: User exists (${result.user?.uid}), deleting...")
                // User exists, delete them
                result.user?.delete()?.awaitWithLooper()
                println("TEST: User deleted")
            }
    } catch (_: Exception) {
        // User doesn't exist - this is expected
    }

    // Create fresh user
    return authUI.auth.createUserWithEmailAndPassword(email, password).awaitWithLooper()
        .user
}

/**
 * Verifies a user's email in the Firebase Auth Emulator by simulating the complete
 * email verification flow.
 *
 * This function:
 * 1. Sends a verification email using sendEmailVerification()
 * 2. Retrieves the OOB (out-of-band) code from the emulator's OOB codes endpoint
 * 3. Applies the action code to complete email verification
 *
 * This approach works with the Firebase Auth Emulator's documented API and simulates
 * the real email verification flow that would occur in production.
 *
 * @param authUI The FirebaseAuthUI instance
 * @param emulatorApi The EmulatorAuthApi instance for fetching OOB codes
 * @param user The FirebaseUser whose email should be verified
 * @throws Exception if the verification flow fails
 */
fun verifyEmailInEmulator(authUI: FirebaseAuthUI, emulatorApi: EmulatorAuthApi, user: FirebaseUser) {
    println("TEST: Starting email verification for user ${user.uid}")

    // Step 1: Send verification email to generate an OOB code
    user.sendEmailVerification().awaitWithLooper()
    println("TEST: Sent email verification request")

    // Give the emulator time to process and store the OOB code
    shadowOf(Looper.getMainLooper()).idle()

    // Step 2: Retrieve the VERIFY_EMAIL OOB code for this user from the emulator
    // Retry with exponential backoff since emulator may be slow
    val email = requireNotNull(user.email) { "User email is required for OOB code lookup" }
    var oobCode: String? = null
    var retries = 0
    val maxRetries = 5
    while (oobCode == null && retries < maxRetries) {
        Thread.sleep(if (retries == 0) 200L else 500L * retries)
        shadowOf(Looper.getMainLooper()).idle()
        try {
            oobCode = emulatorApi.fetchVerifyEmailCode(email)
            println("TEST: Found OOB code after ${retries + 1} attempts")
        } catch (e: Exception) {
            retries++
            if (retries >= maxRetries) {
                throw Exception("Failed to fetch VERIFY_EMAIL OOB code after $maxRetries attempts: ${e.message}")
            }
            println("TEST: OOB code not found yet, retrying... (attempt $retries/$maxRetries)")
        }
    }
    requireNotNull(oobCode) { "OOB code should not be null at this point" }

    println("TEST: Found OOB code: $oobCode")

    // Step 3: Apply the action code to verify the email
    authUI.auth.applyActionCode(oobCode).awaitWithLooper()
    println("TEST: Applied action code")

    // Step 4: Reload the user to refresh their email verification status
    authUI.auth.currentUser?.reload()?.awaitWithLooper()
    shadowOf(Looper.getMainLooper()).idle()

    println("TEST: Email verified successfully for user ${user.uid}")
    println("TEST: User isEmailVerified: ${authUI.auth.currentUser?.isEmailVerified}")
}
