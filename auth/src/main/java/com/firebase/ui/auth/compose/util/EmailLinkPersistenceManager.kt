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

package com.firebase.ui.auth.compose.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "com.firebase.ui.auth.util.data.EmailLinkPersistenceManager")

/**
 * Manages saving/retrieving from DataStore for email link sign in.
 *
 * This class provides persistence for email link authentication sessions, including:
 * - Email address
 * - Session ID for same-device validation
 * - Anonymous user ID for upgrade flows
 * - Social provider credentials for linking flows
 *
 * @since 10.0.0
 */
object EmailLinkPersistenceManager {

    /**
     * Saves email and session information to DataStore for email link sign-in.
     *
     * @param context Android context for DataStore access
     * @param email Email address to save
     * @param sessionId Unique session identifier for same-device validation
     * @param anonymousUserId Optional anonymous user ID for upgrade flows
     */
    suspend fun saveEmail(
        context: Context,
        email: String,
        sessionId: String,
        anonymousUserId: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[AuthProvider.Email.KEY_EMAIL] = email
            prefs[AuthProvider.Email.KEY_SESSION_ID] = sessionId
            prefs[AuthProvider.Email.KEY_ANONYMOUS_USER_ID] = anonymousUserId ?: ""
        }
    }

    /**
     * Saves social provider credential information to DataStore for linking after email link sign-in.
     *
     * This is called when a user attempts to sign in with a social provider (Google/Facebook)
     * but an email link account with the same email already exists. The credential is saved
     * and will be linked after the user completes email link authentication.
     *
     * @param context Android context for DataStore access
     * @param providerType Provider ID ("google.com", "facebook.com", etc.)
     * @param idToken ID token from the provider
     * @param accessToken Access token from the provider (optional, used by Facebook)
     */
    suspend fun saveCredentialForLinking(
        context: Context,
        providerType: String,
        idToken: String?,
        accessToken: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[AuthProvider.Email.KEY_PROVIDER] = providerType
            prefs[AuthProvider.Email.KEY_IDP_TOKEN] = idToken ?: ""
            prefs[AuthProvider.Email.KEY_IDP_SECRET] = accessToken ?: ""
        }
    }

    /**
     * Retrieves session information from DataStore.
     *
     * @param context Android context for DataStore access
     * @return SessionRecord containing saved session data, or null if no session exists
     */
    suspend fun retrieveSessionRecord(context: Context): SessionRecord? {
        val prefs = context.dataStore.data.first()
        val email = prefs[AuthProvider.Email.KEY_EMAIL]
        val sessionId = prefs[AuthProvider.Email.KEY_SESSION_ID]

        if (email == null || sessionId == null) {
            return null
        }

        val anonymousUserId = prefs[AuthProvider.Email.KEY_ANONYMOUS_USER_ID]
        val providerType = prefs[AuthProvider.Email.KEY_PROVIDER]
        val idToken = prefs[AuthProvider.Email.KEY_IDP_TOKEN]
        val accessToken = prefs[AuthProvider.Email.KEY_IDP_SECRET]

        // Rebuild credential if we have provider data
        val credentialForLinking = if (providerType != null && idToken != null) {
            when (providerType) {
                "google.com" -> GoogleAuthProvider.getCredential(idToken, accessToken)
                "facebook.com" -> FacebookAuthProvider.getCredential(accessToken ?: "")
                else -> null
            }
        } else {
            null
        }

        return SessionRecord(
            sessionId = sessionId,
            email = email,
            anonymousUserId = anonymousUserId,
            credentialForLinking = credentialForLinking
        )
    }

    /**
     * Clears all saved data from DataStore.
     *
     * @param context Android context for DataStore access
     */
    suspend fun clear(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(AuthProvider.Email.KEY_SESSION_ID)
            prefs.remove(AuthProvider.Email.KEY_EMAIL)
            prefs.remove(AuthProvider.Email.KEY_ANONYMOUS_USER_ID)
            prefs.remove(AuthProvider.Email.KEY_PROVIDER)
            prefs.remove(AuthProvider.Email.KEY_IDP_TOKEN)
            prefs.remove(AuthProvider.Email.KEY_IDP_SECRET)
        }
    }

    /**
     * Holds the necessary information to complete the email link sign in flow.
     *
     * @property sessionId Unique session identifier for same-device validation
     * @property email Email address for sign-in
     * @property anonymousUserId Optional anonymous user ID for upgrade flows
     * @property credentialForLinking Optional social provider credential to link after sign-in
     */
    data class SessionRecord(
        val sessionId: String,
        val email: String,
        val anonymousUserId: String?,
        val credentialForLinking: AuthCredential?
    )
}
