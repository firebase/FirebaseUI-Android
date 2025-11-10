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

import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential as AndroidPasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

/**
 * Handler for password credential operations using Android's Credential Manager.
 *
 * This class provides methods to save and retrieve password credentials through
 * the system credential manager, which displays native UI prompts to the user.
 *
 * @property context The Android context used for credential operations
 */
class PasswordCredentialHandler(
    private val context: Context
) {
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    /**
     * Saves a password credential to the system credential manager.
     *
     * This method displays a system prompt to the user asking if they want to save
     * the credential. The operation is performed asynchronously using Kotlin coroutines.
     *
     * @param username The username/identifier for the credential
     * @param password The password to save
     * @throws CreateCredentialException if the credential cannot be saved
     * @throws CreateCredentialCancellationException if the user cancels the save operation
     * @throws IllegalArgumentException if username or password is blank
     */
    suspend fun savePassword(username: String, password: String) {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }

        val request = CreatePasswordRequest(
            id = username,
            password = password
        )

        try {
            credentialManager.createCredential(context, request)
        } catch (e: CreateCredentialCancellationException) {
            // User cancelled the save operation
            throw PasswordCredentialCancelledException("User cancelled password save operation", e)
        } catch (e: CreateCredentialException) {
            // Other credential creation errors
            throw PasswordCredentialException("Failed to save password credential", e)
        }
    }

    /**
     * Retrieves a password credential from the system credential manager.
     *
     * This method displays a system prompt showing available credentials for the user
     * to select from. The operation is performed asynchronously using Kotlin coroutines.
     *
     * @return PasswordCredential containing the username and password
     * @throws NoCredentialException if no credentials are available
     * @throws GetCredentialCancellationException if the user cancels the retrieval operation
     * @throws GetCredentialException if the credential cannot be retrieved
     */
    suspend fun getPassword(): PasswordCredential {
        val getPasswordOption = GetPasswordOption()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(getPasswordOption)
            .build()

        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is AndroidPasswordCredential) {
                return PasswordCredential(
                    username = credential.id,
                    password = credential.password
                )
            } else {
                throw PasswordCredentialException("Retrieved credential is not a password credential")
            }
        } catch (e: GetCredentialCancellationException) {
            // User cancelled the retrieval operation
            throw PasswordCredentialCancelledException("User cancelled password retrieval operation", e)
        } catch (e: NoCredentialException) {
            // No credentials available
            throw PasswordCredentialNotFoundException("No password credentials found", e)
        } catch (e: GetCredentialException) {
            // Other credential retrieval errors
            throw PasswordCredentialException("Failed to retrieve password credential", e)
        }
    }
}

/**
 * Base exception for password credential operations.
 */
open class PasswordCredentialException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a password credential operation is cancelled by the user.
 */
class PasswordCredentialCancelledException(
    message: String,
    cause: Throwable? = null
) : PasswordCredentialException(message, cause)

/**
 * Exception thrown when no password credentials are found.
 */
class PasswordCredentialNotFoundException(
    message: String,
    cause: Throwable? = null
) : PasswordCredentialException(message, cause)
