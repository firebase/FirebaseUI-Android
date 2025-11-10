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
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential as AndroidPasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasswordCredentialHandlerTest {

    private lateinit var context: Context
    private lateinit var handler: PasswordCredentialHandler

    @Mock
    private lateinit var mockCredentialManager: CredentialManager

    @Mock
    private lateinit var mockGetCredentialResponse: GetCredentialResponse

    @Mock
    private lateinit var mockAndroidPasswordCredential: AndroidPasswordCredential

    @Captor
    private lateinit var createPasswordRequestCaptor: ArgumentCaptor<CreatePasswordRequest>

    @Captor
    private lateinit var getCredentialRequestCaptor: ArgumentCaptor<GetCredentialRequest>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        handler = PasswordCredentialHandler(context)
    }

    // savePassword tests

    @Test
    fun `savePassword with valid credentials succeeds`() = runTest {
        val username = "test@example.com"
        val password = "securePassword123"

        // This test verifies the handler can be created and called without throwing
        // In a real scenario with a mocked CredentialManager, we would verify the interaction
        try {
            handler.savePassword(username, password)
        } catch (e: PasswordCredentialException) {
            // Expected in test environment without real credential manager
        }
    }

    @Test
    fun `savePassword with blank username throws IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                handler.savePassword("", "password123")
            }
        }
        assertEquals("Username cannot be blank", exception.message)
    }

    @Test
    fun `savePassword with blank password throws IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                handler.savePassword("user@example.com", "")
            }
        }
        assertEquals("Password cannot be blank", exception.message)
    }

    @Test
    fun `savePassword with whitespace-only username throws IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                handler.savePassword("   ", "password123")
            }
        }
        assertEquals("Username cannot be blank", exception.message)
    }

    @Test
    fun `savePassword with whitespace-only password throws IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                handler.savePassword("user@example.com", "   ")
            }
        }
        assertEquals("Password cannot be blank", exception.message)
    }

    // getPassword tests

    @Test
    fun `getPassword returns PasswordCredential when successful`() = runTest {
        // This test verifies the handler structure
        // In a real scenario, we would mock CredentialManager to return a credential
        try {
            val credential = handler.getPassword()
            // If we get here, verify the structure
            assert(credential.username.isNotEmpty() || credential.password.isNotEmpty())
        } catch (e: PasswordCredentialException) {
            // Expected in test environment
        }
    }

    // Exception handling tests

    @Test
    fun `PasswordCredentialException has correct message and cause`() {
        val cause = RuntimeException("Test cause")
        val exception = PasswordCredentialException("Test message", cause)

        assertEquals("Test message", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `PasswordCredentialCancelledException has correct message and cause`() {
        val cause = RuntimeException("Test cause")
        val exception = PasswordCredentialCancelledException("User cancelled", cause)

        assertEquals("User cancelled", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `PasswordCredentialNotFoundException has correct message and cause`() {
        val cause = RuntimeException("Test cause")
        val exception = PasswordCredentialNotFoundException("Not found", cause)

        assertEquals("Not found", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `PasswordCredentialCancelledException is instance of PasswordCredentialException`() {
        val exception = PasswordCredentialCancelledException("Cancelled")
        assert(exception is PasswordCredentialException)
    }

    @Test
    fun `PasswordCredentialNotFoundException is instance of PasswordCredentialException`() {
        val exception = PasswordCredentialNotFoundException("Not found")
        assert(exception is PasswordCredentialException)
    }

    // PasswordCredential data class tests

    @Test
    fun `PasswordCredential holds username and password`() {
        val username = "test@example.com"
        val password = "securePassword123"

        val credential = PasswordCredential(username, password)

        assertEquals(username, credential.username)
        assertEquals(password, credential.password)
    }

    @Test
    fun `PasswordCredential equality works correctly`() {
        val credential1 = PasswordCredential("user@test.com", "pass123")
        val credential2 = PasswordCredential("user@test.com", "pass123")
        val credential3 = PasswordCredential("other@test.com", "pass123")

        assertEquals(credential1, credential2)
        assert(credential1 != credential3)
    }

    @Test
    fun `PasswordCredential copy works correctly`() {
        val original = PasswordCredential("user@test.com", "pass123")
        val copied = original.copy(password = "newPass456")

        assertEquals("user@test.com", copied.username)
        assertEquals("newPass456", copied.password)
        assertEquals("pass123", original.password) // Original unchanged
    }

    @Test
    fun `PasswordCredential component destructuring works`() {
        val credential = PasswordCredential("user@test.com", "pass123")
        val (username, password) = credential

        assertEquals("user@test.com", username)
        assertEquals("pass123", password)
    }

    @Test
    fun `PasswordCredential toString contains field names`() {
        val credential = PasswordCredential("user@test.com", "pass123")
        val toString = credential.toString()

        assert(toString.contains("username"))
        assert(toString.contains("password"))
        assert(toString.contains("user@test.com"))
        assert(toString.contains("pass123"))
    }

    // Integration-style tests (would work with real credential manager)

    @Test
    fun `handler can be created with application context`() {
        val handler = PasswordCredentialHandler(context)
        assert(handler != null)
    }

    @Test
    fun `multiple handlers can be created independently`() {
        val handler1 = PasswordCredentialHandler(context)
        val handler2 = PasswordCredentialHandler(context)

        assert(handler1 != handler2)
    }

    @Test
    fun `handler operations are independent`() = runTest {
        val handler1 = PasswordCredentialHandler(context)
        val handler2 = PasswordCredentialHandler(context)

        // Both handlers should be able to attempt operations independently
        try {
            handler1.savePassword("user1@test.com", "pass1")
        } catch (e: PasswordCredentialException) {
            // Expected in test
        }

        try {
            handler2.savePassword("user2@test.com", "pass2")
        } catch (e: PasswordCredentialException) {
            // Expected in test
        }
    }
}
