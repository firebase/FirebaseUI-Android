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
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager.SessionRecord

/**
 * Mock implementation of [PersistenceManager] for testing.
 * Uses in-memory storage instead of DataStore.
 */
class MockPersistenceManager : PersistenceManager {
    
    private var sessionRecord: SessionRecord? = null
    
    override suspend fun saveEmail(
        context: Context,
        email: String,
        sessionId: String,
        anonymousUserId: String?
    ) {
        sessionRecord = SessionRecord(
            sessionId = sessionId,
            email = email,
            anonymousUserId = anonymousUserId,
            credentialForLinking = sessionRecord?.credentialForLinking
        )
    }
    
    override suspend fun saveCredentialForLinking(
        context: Context,
        providerType: String,
        idToken: String?,
        accessToken: String?
    ) {
        // For mock, we don't reconstruct the credential - just store nulls
        // Real tests can override this method if needed
    }
    
    override suspend fun retrieveSessionRecord(context: Context): SessionRecord? {
        return sessionRecord
    }
    
    override suspend fun clear(context: Context) {
        sessionRecord = null
    }
    
    /**
     * Helper method to set a custom session record for testing.
     */
    fun setSessionRecord(record: SessionRecord?) {
        sessionRecord = record
    }
}
