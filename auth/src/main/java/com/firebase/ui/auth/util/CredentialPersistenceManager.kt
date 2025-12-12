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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.credentialDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "com.firebase.ui.auth.util.CredentialPersistenceManager"
)

/**
 * Manages persistence for credential manager state.
 *
 * This class tracks whether credentials have been saved to the Android Credential Manager
 * to prevent unnecessary credential retrieval attempts when no credentials exist.
 *
 * @since 10.0.0
 */
object CredentialPersistenceManager {

    private val KEY_HAS_SAVED_CREDENTIALS = booleanPreferencesKey("has_saved_credentials")

    /**
     * Marks that credentials have been successfully saved to the credential manager.
     *
     * @param context The Android context
     */
    suspend fun setCredentialsSaved(context: Context) {
        context.credentialDataStore.edit { prefs ->
            prefs[KEY_HAS_SAVED_CREDENTIALS] = true
        }
    }

    /**
     * Checks if credentials have been saved at least once.
     * This prevents unnecessary credential retrieval attempts.
     *
     * @param context The Android context
     * @return true if credentials have been saved, false otherwise
     */
    suspend fun hasSavedCredentials(context: Context): Boolean {
        val prefs = context.credentialDataStore.data.first()
        return prefs[KEY_HAS_SAVED_CREDENTIALS] ?: false
    }

    /**
     * Clears the saved credentials flag.
     * Useful for testing or when user signs out permanently.
     *
     * @param context The Android context
     */
    suspend fun clearSavedCredentialsFlag(context: Context) {
        context.credentialDataStore.edit { prefs ->
            prefs.remove(KEY_HAS_SAVED_CREDENTIALS)
        }
    }
}
