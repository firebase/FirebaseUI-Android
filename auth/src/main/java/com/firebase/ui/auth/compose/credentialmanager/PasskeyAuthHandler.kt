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
 * **Registration Flow:**
 * 1. Server generates a challenge and options
 * 2. Call [registerPasskey] with user ID and display name
 * 3. User interacts with biometric/device authentication
 * 4. Credential is created and can be linked to Firebase
 *
 * **Authentication Flow:**
 * 1. Server generates a challenge
 * 2. Call [authenticateWithPasskey]
 * 3. User authenticates with biometric/device
 * 4. Returns Firebase credential for sign-in
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
     * This method takes the response from a successful passkey registration and links it
     * to the currently signed-in Firebase user account.
     *
     * **Note:** This requires the user to be already authenticated with Firebase.
     *
     * @param credentialResponse The response from [registerPasskey]
     * @return The updated [FirebaseUser] with the linked credential
     * @throws AuthException.InvalidCredentialsException if no user is signed in
     * @throws AuthException if the linking operation fails
     */
    suspend fun linkPasskeyToFirebase(
        credentialResponse: CreatePublicKeyCredentialResponse
    ): FirebaseUser {
        val currentUser = firebaseAuth.currentUser
            ?: throw AuthException.InvalidCredentialsException(
                message = "No user is currently signed in"
            )

        try {
            // Extract the credential from the response
            val authCredential = createFirebaseCredentialFromPasskey(credentialResponse)

            // Link the credential to the current user
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
     * This method takes the credential from a successful passkey authentication and
     * uses it to sign in to Firebase.
     *
     * @param publicKeyCredential The credential from [authenticateWithPasskey]
     * @return The signed-in [FirebaseUser]
     * @throws AuthException if the sign-in operation fails
     */
    suspend fun signInWithPasskey(
        publicKeyCredential: PublicKeyCredential
    ): FirebaseUser {
        try {
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
     * Creates a Firebase credential from a passkey registration response.
     *
     * This is an internal helper method that converts the Android Credential Manager
     * response into a format suitable for Firebase Authentication.
     */
    private fun createFirebaseCredentialFromPasskey(
        response: CreatePublicKeyCredentialResponse
    ): AuthCredential {
        // This is a placeholder implementation.
        // In a real implementation, you would need to:
        // 1. Extract the registration response data
        // 2. Send it to your server for verification
        // 3. Have the server create a Firebase custom token
        // 4. Use the custom token to create an AuthCredential
        throw NotImplementedError(
            "Passkey to Firebase credential conversion requires server-side implementation"
        )
    }

    /**
     * Creates a Firebase credential from a passkey authentication result.
     *
     * This is an internal helper method that converts the passkey authentication
     * assertion into a format suitable for Firebase Authentication.
     */
    private fun createFirebaseCredentialFromPasskeyAuth(
        credential: PublicKeyCredential
    ): AuthCredential {
        // This is a placeholder implementation.
        // In a real implementation, you would need to:
        // 1. Extract the authentication assertion
        // 2. Send it to your server for verification
        // 3. Have the server create a Firebase custom token
        // 4. Use the custom token to create an AuthCredential
        throw NotImplementedError(
            "Passkey authentication to Firebase credential conversion requires server-side implementation"
        )
    }

    companion object {
        /**
         * Creates a JSON request for passkey registration.
         *
         * This is a helper method to generate the required JSON format for passkey registration.
         * In a production app, this should be generated by your server.
         *
         * @param challenge The base64-encoded challenge from your server
         * @param userId The user's unique identifier
         * @param userName The user's username (typically email)
         * @param displayName The user's display name
         * @param rpId The Relying Party ID (typically your domain)
         * @param rpName The Relying Party name
         * @return JSON string suitable for [registerPasskey]
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
            val request = JSONObject().apply {
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
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -7) // ES256
                    })
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("alg", -257) // RS256
                    })
                })
                put("timeout", 60000)
                put("attestation", "none")
                put("authenticatorSelection", JSONObject().apply {
                    put("authenticatorAttachment", "platform")
                    put("requireResidentKey", true)
                    put("residentKey", "required")
                    put("userVerification", "required")
                })
            }
            return request.toString()
        }

        /**
         * Creates a JSON request for passkey authentication.
         *
         * This is a helper method to generate the required JSON format for passkey authentication.
         * In a production app, this should be generated by your server.
         *
         * @param challenge The base64-encoded challenge from your server
         * @param rpId The Relying Party ID (typically your domain)
         * @return JSON string suitable for [authenticateWithPasskey]
         */
        @JvmStatic
        fun createAuthenticationRequestJson(
            challenge: String,
            rpId: String
        ): String {
            val request = JSONObject().apply {
                put("challenge", challenge)
                put("timeout", 60000)
                put("rpId", rpId)
                put("userVerification", "required")
            }
            return request.toString()
        }
    }
}
