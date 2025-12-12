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

package com.firebase.ui.auth.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.signInPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "com.firebase.ui.auth.util.SignInPreferenceManager"
)

/**
 * Manages persistence for the last-used sign-in method.
 *
 * This class tracks which authentication provider was last used to sign in,
 * along with the user identifier (email, phone number, etc.). This enables
 * a better UX by showing "Continue as [identifier]" with the last-used provider
 * prominently on the method picker screen.
 *
 * @since 10.0.0
 */
object SignInPreferenceManager {

    private val KEY_LAST_PROVIDER_ID = stringPreferencesKey("last_provider_id")
    private val KEY_LAST_IDENTIFIER = stringPreferencesKey("last_identifier")
    private val KEY_LAST_TIMESTAMP = longPreferencesKey("last_timestamp")

    /**
     * Saves the last-used sign-in method and user identifier.
     *
     * This should be called after a successful sign-in to track the user's
     * preferred sign-in method.
     *
     * @param context The Android context
     * @param providerId The provider ID (e.g., "google.com", "facebook.com", "password", "phone")
     * @param identifier The user identifier (email for social/email auth, phone number for phone auth)
     */
    suspend fun saveLastSignIn(
        context: Context,
        providerId: String,
        identifier: String?
    ) {
        context.signInPreferenceDataStore.edit { prefs ->
            prefs[KEY_LAST_PROVIDER_ID] = providerId
            identifier?.let { prefs[KEY_LAST_IDENTIFIER] = it }
            prefs[KEY_LAST_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Retrieves the last-used sign-in preference.
     *
     * @param context The Android context
     * @return [SignInPreference] containing the last-used provider and identifier, or null if none
     */
    suspend fun getLastSignIn(context: Context): SignInPreference? {
        val prefs = context.signInPreferenceDataStore.data.first()
        val providerId = prefs[KEY_LAST_PROVIDER_ID]
        val identifier = prefs[KEY_LAST_IDENTIFIER]
        val timestamp = prefs[KEY_LAST_TIMESTAMP]

        return if (providerId != null && timestamp != null) {
            SignInPreference(
                providerId = providerId,
                identifier = identifier,
                timestamp = timestamp
            )
        } else {
            null
        }
    }

    /**
     * Clears the saved sign-in preference.
     *
     * This should be called when the user signs out permanently or
     * when resetting authentication state.
     *
     * @param context The Android context
     */
    suspend fun clearLastSignIn(context: Context) {
        context.signInPreferenceDataStore.edit { prefs ->
            prefs.remove(KEY_LAST_PROVIDER_ID)
            prefs.remove(KEY_LAST_IDENTIFIER)
            prefs.remove(KEY_LAST_TIMESTAMP)
        }
    }

    /**
     * Data class representing a saved sign-in preference.
     *
     * @property providerId The provider ID (e.g., "google.com", "facebook.com", "password", "phone")
     * @property identifier The user identifier (email, phone number, etc.), may be null
     * @property timestamp The timestamp when this preference was saved
     */
    data class SignInPreference(
        val providerId: String,
        val identifier: String?,
        val timestamp: Long
    )
}
