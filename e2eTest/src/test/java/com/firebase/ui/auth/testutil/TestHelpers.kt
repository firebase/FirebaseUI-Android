package com.firebase.ui.auth.testutil

import android.app.Activity
import android.os.Looper
import android.util.Base64
import com.firebase.ui.auth.FirebaseAuthUI
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

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

/**
 * Enrolls [user] in SMS multi-factor authentication, so a test can start from an account that is
 * challenged for a second factor at sign-in.
 *
 * Takes the same three steps [com.firebase.ui.auth.mfa.SmsEnrollmentHandler] does — open a
 * multi-factor session, verify a phone number against it, enroll the resulting assertion — but
 * from Tasks this thread can pump with [awaitWithLooper], rather than the handler's suspend
 * functions, whose callbacks arrive on the paused main looper a `runBlocking` here would occupy.
 *
 * The emulator rejects enrollment for a user whose email is unverified (`UNVERIFIED_EMAIL`), so
 * pair this with [verifyEmailInEmulator], and for an anonymous, phone or custom-token first factor
 * (`UNSUPPORTED_FIRST_FACTOR`).
 *
 * @param phoneNumber The second factor's number in E.164 format (e.g. "+15551234567")
 */
fun enrollSmsFactorInEmulator(
    activity: Activity,
    authUI: FirebaseAuthUI,
    emulatorApi: EmulatorAuthApi,
    user: FirebaseUser,
    phoneNumber: String,
) {
    println("TEST: Enrolling SMS factor $phoneNumber for user ${user.uid}")
    val session = user.multiFactor.session.awaitWithLooper()

    val verificationId = TaskCompletionSource<String>()
    val options = PhoneAuthOptions.newBuilder(authUI.auth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(SMS_VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .setActivity(activity)
        .setMultiFactorSession(session)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // The emulator always answers with a session to redeem a code against, so the
            // instant-verification callback firing here means the setup no longer matches the
            // flow it is standing in for.
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                verificationId.trySetException(
                    IllegalStateException(
                        "Emulator auto-verified $phoneNumber instead of sending a code"
                    )
                )
            }

            override fun onVerificationFailed(e: FirebaseException) {
                verificationId.trySetException(e)
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                verificationId.trySetResult(id)
            }
        })
        .build()
    PhoneAuthProvider.verifyPhoneNumber(options)

    // onCodeSent carries the emulator's response to the enrollment start, so by the time it lands
    // the code is already readable — no polling needed.
    val id = verificationId.task.awaitWithLooper()
    val code = emulatorApi.fetchVerifyPhoneCode(phoneNumber)
    println("TEST: Enrollment code for $phoneNumber is $code")

    val credential = PhoneAuthProvider.getCredential(id, code)
    user.multiFactor.enroll(PhoneMultiFactorGenerator.getAssertion(credential), "SMS")
        .awaitWithLooper()
    println("TEST: Enrolled factors: ${user.multiFactor.enrolledFactors.size}")
}

/** Matches [com.firebase.ui.auth.mfa.SmsEnrollmentHandler.VERIFICATION_TIMEOUT_SECONDS]. */
private const val SMS_VERIFICATION_TIMEOUT_SECONDS = 60L

fun generateMockGoogleIdToken(
    email: String,
    sub: String = "test-user-id",
    name: String? = null,
    photoUrl: String? = null,
): String {
    val header = """{"alg":"RS256","kid":"test"}"""
    val payload = buildString {
        append("{")
        append("\"iss\":\"https://accounts.google.com\",")
        append("\"aud\":\"test-client-id\",")
        append("\"sub\":\"$sub\",")
        append("\"email\":\"$email\",")
        append("\"email_verified\":true")
        name?.let { append(",\"name\":\"$it\"") }
        photoUrl?.let { append(",\"picture\":\"$it\"") }
        append(",\"iat\":1689600000,\"exp\":1689603600")
        append("}")
    }
    val encodedHeader = Base64.encodeToString(header.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    val encodedPayload = Base64.encodeToString(payload.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    return "$encodedHeader.$encodedPayload.mock-signature"
}
