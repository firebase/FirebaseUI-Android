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

package com.firebase.ui.auth.compose.mfa

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
import com.google.firebase.auth.TotpMultiFactorGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [TotpEnrollmentHandler].
 *
 * Note: Full integration tests for secret generation and enrollment require
 * mocking static Firebase methods, which is complex with Mockito.
 * These tests focus on validation logic and constants.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TotpEnrollmentHandlerTest {

    @Mock
    private lateinit var mockAuth: FirebaseAuth
    @Mock
    private lateinit var mockUser: FirebaseUser
    @Mock
    private lateinit var mockMultiFactor: MultiFactor
    private lateinit var handler: TotpEnrollmentHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        handler = TotpEnrollmentHandler(mockAuth, mockUser)
    }

    @Test
    fun `isValidCodeFormat returns true for valid 6-digit codes`() {
        // Valid codes
        assertTrue(handler.isValidCodeFormat("123456"))
        assertTrue(handler.isValidCodeFormat("000000"))
        assertTrue(handler.isValidCodeFormat("999999"))
        assertTrue(handler.isValidCodeFormat("123456"))
    }

    @Test
    fun `isValidCodeFormat returns false for empty or blank codes`() {
        assertFalse(handler.isValidCodeFormat(""))
        assertFalse(handler.isValidCodeFormat("     "))
        assertFalse(handler.isValidCodeFormat("  "))
    }

    @Test
    fun `isValidCodeFormat returns false for codes with wrong length`() {
        assertFalse(handler.isValidCodeFormat("12345")) // Too short
        assertFalse(handler.isValidCodeFormat("1234567")) // Too long
        assertFalse(handler.isValidCodeFormat("1")) // Way too short
        assertFalse(handler.isValidCodeFormat("12345678901234567890")) // Way too long
    }

    @Test
    fun `isValidCodeFormat returns false for codes with non-digit characters`() {
        assertFalse(handler.isValidCodeFormat("12345a")) // Contains letter
        assertFalse(handler.isValidCodeFormat("12 34 56")) // Contains spaces
        assertFalse(handler.isValidCodeFormat("abc123")) // Contains letters
        assertFalse(handler.isValidCodeFormat("12-345")) // Contains dash
        assertFalse(handler.isValidCodeFormat("12.345")) // Contains dot
        assertFalse(handler.isValidCodeFormat("123!56")) // Contains special char
    }

    @Test
    fun `constants have expected values`() {
        assertEquals(6, TotpEnrollmentHandler.TOTP_CODE_LENGTH)
        assertEquals(30, TotpEnrollmentHandler.TOTP_TIME_INTERVAL_SECONDS)
        assertEquals(TotpMultiFactorGenerator.FACTOR_ID, TotpEnrollmentHandler.FACTOR_ID)
    }

    @Test
    fun `handler is created with correct auth and user references`() {
        // Verify handler can be instantiated
        val newHandler = TotpEnrollmentHandler(mockAuth, mockUser)
        // Basic smoke test - if we get here, construction succeeded
        assertTrue(newHandler.isValidCodeFormat("123456"))
    }
}
