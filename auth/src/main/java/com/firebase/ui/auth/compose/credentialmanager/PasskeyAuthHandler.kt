/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.compose.credentialmanager

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.firebase.ui.auth.compose.AuthException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Handler for passkey-based authentication using Android Credential Manager API and Firebase Auth.
 *
 * This class provides methods to register and authenticate users with passkeys (WebAuthn credentials),
 * integrating with both Android's Credential Manager and Firebase Authentication.
 *
 * **IMPORTANT - Production Requirements:**
 *
 * Firebase Auth does not have native passkey support yet. To use passkeys with Firebase in production,
 * you MUST implement a server-side component that:
 * 1. Generates WebAuthn challenges
 * 2. Verifies passkey registration/authentication responses
 * 3. Creates Firebase custom tokens after successful verification
 * 4. Returns the custom token to your client
 *
 * The [linkPasskeyToFirebase] and [signInWithPasskey] methods are currently incomplete and will throw
 * errors until you implement the server-side credential verification logic.
 *
 * **Registration Flow:**
 * 1. Your server generates a challenge and options
 * 2. Call [registerPasskey] with the server-provided challenge
 * 3. User authenticates with biometric/device
 * 4. Send response to your server for verification
 * 5. Server creates a Firebase custom token
 * 6. Use the custom token to link or sign in to Firebase
 *
 * **Authentication Flow:**
 * 1. Your server generates a challenge
 * 2. Call [authenticateWithPasskey] with the challenge
 * 3. User authenticates with biometric/device
 * 4. Send response to your server for verification
 * 5. Server creates a Firebase custom token
 * 6. Use the custom token to sign in to Firebase
 *
 * @property context The Android context for credential operations
 * @property firebaseAuth The Firebase Auth instance
 *
 * @since 10.0.0
 */
class PasskeyAuthHandler(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    /**
     * Registers a new passkey for the user.
     *
     * This method creates a new WebAuthn credential (passkey) and stores it in the
     * device's credential manager. The passkey can then be used for future authentication.
     *
     * **Requirements:**
     * - User must have biometric or device lock screen set up
     * - App must have proper configuration in Firebase Console
     * - Request JSON must include valid challenge and RP information
     *
     * **Example:**
     * ```kotlin
     * val handler = PasskeyAuthHandler(context)
     * try {
     *     val response = handler.registerPasskey(
     *         userId = "user123",
     *         displayName = "John Doe",
     *         requestJson = serverGeneratedRequestJson
     *     )
     *     // Link to Firebase or store credential ID
     * } catch (e: AuthException) {
     *     // Handle error
     * }
     * ```
     *
     * @param userId The unique identifier for the user
     * @param displayName The user's display name
     * @param requestJson JSON string containing the credential creation options from the server
     * @return [CreatePublicKeyCredentialResponse] containing the newly created credential
     * @throws AuthException.AuthCancelledException if the user cancels the operation
     * @throws AuthException.InvalidCredentialsException if the configuration is invalid
     * @throws AuthException.UnknownException for other errors
     */
    @SuppressLint("PublicKeyCredential")
    suspend fun registerPasskey(
        userId: String,
        displayName: String,
        requestJson: String
    ): CreatePublicKeyCredentialResponse {
        try {
            val request = CreatePublicKeyCredentialRequest(
                requestJson = requestJson,
                preferImmediatelyAvailableCredentials = false
            )

            val response = credentialManager.createCredential(
                context = context,
                request = request
            )

            return response as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialCancellationException) {
            throw AuthException.AuthCancelledException(
                message = "Passkey registration was cancelled by the user",
                cause = e
            )
        } catch (e: CreateCredentialInterruptedException) {
            throw AuthException.AuthCancelledException(
                message = "Passkey registration was interrupted",
                cause = e
            )
        } catch (e: CreateCredentialProviderConfigurationException) {
            throw AuthException.InvalidCredentialsException(
                message = "Invalid passkey configuration. Please check your setup.",
                cause = e
            )
        } catch (e: CreateCredentialUnknownException) {
            throw AuthException.UnknownException(
                message = "Unknown error during passkey registration: ${e.message}",
                cause = e
            )
        } catch (e: CreateCredentialException) {
            throw AuthException.UnknownException(
                message = "Error creating passkey: ${e.message}",
                cause = e
            )
        } catch (e: Exception) {
            throw AuthException.UnknownException(
                message = "Unexpected error during passkey registration: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Authenticates the user with a passkey.
     *
     * This method retrieves and uses an existing passkey to authenticate the user.
     * The returned response contains the authentication assertion that can be verified
     * by the server and used to sign in with Firebase.
     *
     * **Example:**
     * ```kotlin
     * val handler = PasskeyAuthHandler(context)
     * try {
     *     val credential = handler.authenticateWithPasskey(
     *         requestJson = serverGeneratedChallengeJson
     *     )
     *     // Send to server for verification and get Firebase custom token
     *     // Or use credential directly with Firebase
     * } catch (e: AuthException) {
     *     // Handle error
     * }
     * ```
     *
     * @param requestJson JSON string containing the credential request options from the server
     * @return [PublicKeyCredential] containing the authentication assertion
     * @throws AuthException.UserNotFoundException if no passkey is found
     * @throws AuthException.AuthCancelledException if the user cancels the operation
     * @throws AuthException.InvalidCredentialsException if the configuration is invalid
     * @throws AuthException.UnknownException for other errors
     */
    suspend fun authenticateWithPasskey(
        requestJson: String
    ): PublicKeyCredential {
        try {
            val getCredentialOption = GetPublicKeyCredentialOption(
                requestJson = requestJson,
                clientDataHash = null
            )

            val getCredentialRequest = GetCredentialRequest(
                credentialOptions = listOf(getCredentialOption)
            )

            val response: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = getCredentialRequest
            )

            val credential = response.credential
            if (credential is PublicKeyCredential) {
                return credential
            } else {
                throw AuthException.InvalidCredentialsException(
                    message = "Retrieved credential is not a passkey"
                )
            }
        } catch (e: GetCredentialCancellationException) {
            throw AuthException.AuthCancelledException(
                message = "Passkey authentication was cancelled by the user",
                cause = e
            )
        } catch (e: GetCredentialInterruptedException) {
            throw AuthException.AuthCancelledException(
                message = "Passkey authentication was interrupted",
                cause = e
            )
        } catch (e: NoCredentialException) {
            throw AuthException.UserNotFoundException(
                message = "No passkey found for this account",
                cause = e
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            throw AuthException.InvalidCredentialsException(
                message = "Invalid passkey configuration. Please check your setup.",
                cause = e
            )
        } catch (e: GetCredentialUnknownException) {
            throw AuthException.UnknownException(
                message = "Unknown error during passkey authentication: ${e.message}",
                cause = e
            )
        } catch (e: GetCredentialException) {
            throw AuthException.UnknownException(
                message = "Error retrieving passkey: ${e.message}",
                cause = e
            )
        } catch (e: Exception) {
            throw AuthException.UnknownException(
                message = "Unexpected error during passkey authentication: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Links a passkey credential to the current Firebase user.
     *
     * **TODO: This method requires server-side implementation.**
     *
     * This method is currently incomplete. To make it work, you need to:
     * 1. Implement server-side verification of the passkey registration response
     * 2. Have your server create a Firebase custom token after verification
     * 3. Complete the [createFirebaseCredentialFromPasskey] method below
     *
     * Once implemented, this will link the passkey to the currently signed-in Firebase user.
     *
     * @param credentialResponse The response from [registerPasskey]
     * @return The updated [FirebaseUser] with the linked credential
     * @throws AuthException.InvalidCredentialsException if no user is signed in
     * @throws AuthException.UnknownException currently always throws until implemented
     */
    suspend fun linkPasskeyToFirebase(
        credentialResponse: CreatePublicKeyCredentialResponse
    ): FirebaseUser {
        val currentUser = firebaseAuth.currentUser
            ?: throw AuthException.InvalidCredentialsException(
                message = "No user is currently signed in"
            )

        try {
            // TODO: Implement server-side verification flow
            val authCredential = createFirebaseCredentialFromPasskey(credentialResponse)

            val authResult = currentUser.linkWithCredential(authCredential).await()

            return authResult.user
                ?: throw AuthException.UnknownException("Failed to link passkey to user")
        } catch (e: FirebaseAuthException) {
            throw AuthException.from(e)
        } catch (e: Exception) {
            if (e is AuthException) throw e
            throw AuthException.UnknownException(
                message = "Error linking passkey to Firebase: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Signs in to Firebase using a passkey authentication result.
     *
     * **TODO: This method requires server-side implementation.**
     *
     * This method is currently incomplete. To make it work, you need to:
     * 1. Implement server-side verification of the passkey authentication assertion
     * 2. Have your server create a Firebase custom token after verification
     * 3. Complete the [createFirebaseCredentialFromPasskeyAuth] method below
     *
     * Once implemented, this will sign in the user to Firebase using their passkey.
     *
     * @param publicKeyCredential The credential from [authenticateWithPasskey]
     * @return The signed-in [FirebaseUser]
     * @throws AuthException.UnknownException currently always throws until implemented
     */
    suspend fun signInWithPasskey(
        publicKeyCredential: PublicKeyCredential
    ): FirebaseUser {
        try {
            // TODO: Implement server-side verification flow
            val authCredential = createFirebaseCredentialFromPasskeyAuth(publicKeyCredential)

            val authResult = firebaseAuth.signInWithCredential(authCredential).await()

            return authResult.user
                ?: throw AuthException.UnknownException("Sign in succeeded but no user returned")
        } catch (e: FirebaseAuthException) {
            throw AuthException.from(e)
        } catch (e: Exception) {
            if (e is AuthException) throw e
            throw AuthException.UnknownException(
                message = "Error signing in with passkey: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Converts a passkey registration response to a Firebase credential.
     *
     * **TODO: Implement this method with your server-side verification.**
     *
     * To complete this method, you need to:
     * 1. Extract the credential registration data from the response:
     *    ```kotlin
     *    val registrationResponseJson = response.registrationResponseJson
     *    ```
     * 2. Send this JSON to your server endpoint (e.g., POST to /api/verify-passkey-registration)
     * 3. On your server:
     *    - Verify the WebAuthn registration response using a library like @simplewebauthn/server
     *    - Store the credential ID and public key for the user
     *    - Create a Firebase custom token using the Firebase Admin SDK
     *    - Return the custom token to the client
     * 4. Create a Firebase credential using the custom token:
     *    ```kotlin
     *    return FirebaseAuth.getInstance().signInWithCustomToken(customToken).await().user
     *    ```
     *
     * @param response The passkey registration response from Android Credential Manager
     * @return Firebase AuthCredential that can be used to link or sign in
     * @throws AuthException.UnknownException until this method is properly implemented
     */
    private fun createFirebaseCredentialFromPasskey(
        response: CreatePublicKeyCredentialResponse
    ): AuthCredential {
        // TODO: Replace this with your server verification implementation
        throw AuthException.UnknownException(
            message = "Passkey-to-Firebase credential conversion is not yet implemented. " +
                    "You must set up server-side WebAuthn verification and Firebase custom token generation. " +
                    "See the method documentation for implementation details."
        )
    }

    /**
     * Converts a passkey authentication response to a Firebase credential.
     *
     * **TODO: Implement this method with your server-side verification.**
     *
     * To complete this method, you need to:
     * 1. Extract the authentication assertion from the credential:
     *    ```kotlin
     *    val authenticationResponseJson = credential.authenticationResponseJson
     *    ```
     * 2. Send this JSON to your server endpoint (e.g., POST to /api/verify-passkey-auth)
     * 3. On your server:
     *    - Verify the WebAuthn authentication assertion using a library like @simplewebauthn/server
     *    - Validate the signature against the stored public key
     *    - Create a Firebase custom token using the Firebase Admin SDK
     *    - Return the custom token to the client
     * 4. Create a Firebase credential using the custom token:
     *    ```kotlin
     *    return FirebaseAuth.getInstance().signInWithCustomToken(customToken).await().user
     *    ```
     *
     * @param credential The passkey authentication credential from Android Credential Manager
     * @return Firebase AuthCredential that can be used to sign in
     * @throws AuthException.UnknownException until this method is properly implemented
     */
    private fun createFirebaseCredentialFromPasskeyAuth(
        credential: PublicKeyCredential
    ): AuthCredential {
        // TODO: Replace this with your server verification implementation
        throw AuthException.UnknownException(
            message = "Passkey authentication-to-Firebase credential conversion is not yet implemented. " +
                    "You must set up server-side WebAuthn verification and Firebase custom token generation. " +
                    "See the method documentation for implementation details."
        )
    }

    companion object {
        /**
         * Creates a JSON request for passkey registration.
         *
         * **WARNING: For testing/development only. Production apps should generate this on the server.**
         *
         * This helper method generates a WebAuthn PublicKeyCredentialCreationOptions JSON.
         * However, in production:
         * - The challenge MUST be generated on your server with cryptographically secure randomness
         * - The server should persist the challenge to verify the response later
         * - The server controls security parameters (timeout, attestation requirements, etc.)
         *
         * This client-side method is provided for testing and prototyping only.
         *
         * @param challenge Base64-encoded challenge (should come from your server)
         * @param userId Unique user identifier
         * @param userName Username (typically email address)
         * @param displayName User's display name
         * @param rpId Relying Party ID (your domain, e.g., "example.com")
         * @param rpName Relying Party name (your app name)
         * @return WebAuthn PublicKeyCredentialCreationOptions as JSON string
         */
        @JvmStatic
        fun createRegistrationRequestJson(
            challenge: String,
            userId: String,
            userName: String,
            displayName: String,
            rpId: String,
            rpName: String
        ): String {
            return JSONObject().apply {
                put("challenge", challenge)
                put("rp", JSONObject().apply {
                    put("name", rpName)
                    put("id", rpId)
                })
                put("user", JSONObject().apply {
                    put("id", userId)
                    put("name", userName)
                    put("displayName", displayName)
                })
                put("pubKeyCredParams", org.json.JSONArray().apply {
                    // ES256 (preferred) - ECDSA with SHA-256
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -7)
                    })
                    // RS256 (fallback) - RSASSA-PKCS1-v1_5 with SHA-256
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -257)
                    })
                })
                put("timeout", 60000) // 60 seconds
                put("attestation", "none") // Don't require attestation for privacy
                put("authenticatorSelection", JSONObject().apply {
                    put("authenticatorAttachment", "platform") // Device-bound passkey
                    put("requireResidentKey", true) // Discoverable credential
                    put("residentKey", "required")
                    put("userVerification", "required") // Biometric or PIN required
                })
            }.toString()
        }

        /**
         * Creates a JSON request for passkey authentication.
         *
         * **WARNING: For testing/development only. Production apps should generate this on the server.**
         *
         * This helper method generates a WebAuthn PublicKeyCredentialRequestOptions JSON.
         * However, in production:
         * - The challenge MUST be generated on your server with cryptographically secure randomness
         * - The server should persist the challenge to verify the response later
         * - The server may want to specify allowed credentials for better UX
         *
         * This client-side method is provided for testing and prototyping only.
         *
         * @param challenge Base64-encoded challenge (should come from your server)
         * @param rpId Relying Party ID (your domain, e.g., "example.com")
         * @return WebAuthn PublicKeyCredentialRequestOptions as JSON string
         */
        @JvmStatic
        fun createAuthenticationRequestJson(
            challenge: String,
            rpId: String
        ): String {
            return JSONObject().apply {
                put("challenge", challenge)
                put("timeout", 60000) // 60 seconds
                put("rpId", rpId)
                put("userVerification", "required") // Biometric or PIN required
            }.toString()
        }
    }
}
