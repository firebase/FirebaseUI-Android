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

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SmsEnrollmentHandler].
 *
 * Note: Full integration tests for SMS sending and enrollment require
 * mocking static Firebase methods and Android components, which is complex.
 * These tests focus on validation logic, constants, and utility functions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SmsEnrollmentHandlerTest {

    @Mock
    private lateinit var mockAuth: FirebaseAuth
    @Mock
    private lateinit var mockUser: FirebaseUser
    @Mock
    private lateinit var mockMultiFactor: MultiFactor
    @Mock
    private lateinit var mockActivity: Activity
    private lateinit var handler: SmsEnrollmentHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockActivity = Robolectric.buildActivity(Activity::class.java).create().get()
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        handler = SmsEnrollmentHandler(mockAuth, mockUser, mockActivity)
    }

    // isValidCodeFormat tests

    @Test
    fun `isValidCodeFormat returns true for valid 6-digit codes`() {
        assertTrue(handler.isValidCodeFormat("123456"))
        assertTrue(handler.isValidCodeFormat("000000"))
        assertTrue(handler.isValidCodeFormat("999999"))
        assertTrue(handler.isValidCodeFormat("654321"))
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

    // isValidPhoneNumber tests

    @Test
    fun `isValidPhoneNumber returns true for valid E164 phone numbers`() {
        assertTrue(handler.isValidPhoneNumber("+1234567890")) // US
        assertTrue(handler.isValidPhoneNumber("+447911123456")) // UK
        assertTrue(handler.isValidPhoneNumber("+33612345678")) // France
        assertTrue(handler.isValidPhoneNumber("+861234567890")) // China
        assertTrue(handler.isValidPhoneNumber("+5511987654321")) // Brazil
    }

    @Test
    fun `isValidPhoneNumber returns false for numbers without plus sign`() {
        assertFalse(handler.isValidPhoneNumber("1234567890"))
        assertFalse(handler.isValidPhoneNumber("447911123456"))
    }

    @Test
    fun `isValidPhoneNumber returns false for numbers starting with zero after plus`() {
        assertFalse(handler.isValidPhoneNumber("+0234567890"))
        assertFalse(handler.isValidPhoneNumber("+0447911123456"))
    }

    @Test
    fun `isValidPhoneNumber returns false for numbers that are too short`() {
        assertFalse(handler.isValidPhoneNumber("+1")) // Too short
        assertFalse(handler.isValidPhoneNumber("+12")) // Still too short
    }

    @Test
    fun `isValidPhoneNumber returns false for numbers that are too long`() {
        assertFalse(handler.isValidPhoneNumber("+12345678901234567")) // More than 15 digits
    }

    @Test
    fun `isValidPhoneNumber returns false for numbers with non-digit characters`() {
        assertFalse(handler.isValidPhoneNumber("+1 234 567 890")) // Spaces
        assertFalse(handler.isValidPhoneNumber("+1-234-567-890")) // Dashes
        assertFalse(handler.isValidPhoneNumber("+1(234)567890")) // Parentheses
        assertFalse(handler.isValidPhoneNumber("+1.234.567.890")) // Dots
    }

    @Test
    fun `isValidPhoneNumber returns false for empty or blank numbers`() {
        assertFalse(handler.isValidPhoneNumber(""))
        assertFalse(handler.isValidPhoneNumber("   "))
    }

    // Constants tests

    @Test
    fun `constants have expected values`() {
        assertEquals(6, SmsEnrollmentHandler.SMS_CODE_LENGTH)
        assertEquals(60L, SmsEnrollmentHandler.VERIFICATION_TIMEOUT_SECONDS)
        assertEquals(30, SmsEnrollmentHandler.RESEND_DELAY_SECONDS)
        assertEquals(PhoneMultiFactorGenerator.FACTOR_ID, SmsEnrollmentHandler.FACTOR_ID)
    }

    @Test
    fun `handler is created with correct auth and user references`() {
        // Verify handler can be instantiated
        val newHandler = SmsEnrollmentHandler(mockAuth, mockUser, mockActivity)
        // Basic smoke test - if we get here, construction succeeded
        assertTrue(newHandler.isValidCodeFormat("123456"))
    }

    // maskPhoneNumber tests

    @Test
    fun `maskPhoneNumber masks US phone numbers correctly`() {
        val masked = maskPhoneNumber("+1234567890")
        assertTrue(masked.startsWith("+1"))
        assertTrue(masked.endsWith("890"))
        assertTrue(masked.contains("•"))
        assertEquals("+1••••••890", masked)
    }

    @Test
    fun `maskPhoneNumber masks UK phone numbers correctly`() {
        val masked = maskPhoneNumber("+447911123456")
        assertTrue(masked.startsWith("+44"))
        assertTrue(masked.endsWith("456"))
        assertTrue(masked.contains("•"))
        // UK number: 13 chars, shows last 3 digits
        assertEquals("+44•••••••456", masked)
    }

    @Test
    fun `maskPhoneNumber masks French phone numbers correctly`() {
        val masked = maskPhoneNumber("+33612345678")
        assertTrue(masked.startsWith("+33"))
        assertTrue(masked.endsWith("678"))
        assertTrue(masked.contains("•"))
        // French number: 12 chars, shows last 3 digits
        assertEquals("+33••••••678", masked)
    }

    @Test
    fun `maskPhoneNumber handles short phone numbers`() {
        val short = "+1234567"
        val masked = maskPhoneNumber(short)
        assertTrue(masked.startsWith("+1"))
        assertTrue(masked.contains("•"))
    }

    @Test
    fun `maskPhoneNumber returns original for invalid numbers`() {
        assertEquals("1234567890", maskPhoneNumber("1234567890")) // No +
        assertEquals("abc", maskPhoneNumber("abc")) // Not a number
        assertEquals("+123", maskPhoneNumber("+123")) // Too short
    }

    @Test
    fun `maskPhoneNumber masks different country codes correctly`() {
        // Single-digit country code (US)
        val us = maskPhoneNumber("+1234567890")
        assertTrue(us.startsWith("+1"))

        // Two-digit country code (UK)
        val uk = maskPhoneNumber("+447911123456")
        assertTrue(uk.startsWith("+44"))

        // Three-digit country code (less common, but handled)
        val threeDigit = maskPhoneNumber("+8861234567890")
        assertTrue(threeDigit.startsWith("+88"))
    }
}

/**
 * Unit tests for [SmsEnrollmentSession].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SmsEnrollmentSessionTest {

    @Mock
    private lateinit var mockForceResendingToken: PhoneAuthProvider.ForceResendingToken

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `session holds all properties correctly`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id-123",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = 1000000L
        )

        assertEquals("test-id-123", session.verificationId)
        assertEquals("+1234567890", session.phoneNumber)
        assertEquals(mockForceResendingToken, session.forceResendingToken)
        assertEquals(1000000L, session.sentAt)
    }

    @Test
    fun `getMaskedPhoneNumber returns masked version`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = null,
            sentAt = System.currentTimeMillis()
        )

        val masked = session.getMaskedPhoneNumber()
        // 11 char number shows last 3 digits
        assertEquals("+1••••••890", masked)
    }

    @Test
    fun `canResend returns false immediately after sending`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = System.currentTimeMillis()
        )

        assertFalse(session.canResend())
    }

    @Test
    fun `canResend returns true after delay has passed`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = System.currentTimeMillis() - 31_000 // 31 seconds ago
        )

        assertTrue(session.canResend())
    }

    @Test
    fun `canResend works with custom delay`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = System.currentTimeMillis() - 5_000 // 5 seconds ago
        )

        assertFalse(session.canResend(10)) // 10 second delay
        assertTrue(session.canResend(4)) // 4 second delay
    }

    @Test
    fun `getRemainingResendSeconds returns correct value`() {
        val now = System.currentTimeMillis()
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = now - 10_000 // 10 seconds ago
        )

        val remaining = session.getRemainingResendSeconds(30)
        // Should be around 20 seconds (30 - 10)
        assertTrue(remaining in 19..21)
    }

    @Test
    fun `getRemainingResendSeconds returns 0 when resend is allowed`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = System.currentTimeMillis() - 35_000 // 35 seconds ago
        )

        assertEquals(0, session.getRemainingResendSeconds(30))
    }

    @Test
    fun `getRemainingResendSeconds works with custom delay`() {
        val now = System.currentTimeMillis()
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = mockForceResendingToken,
            sentAt = now - 3_000 // 3 seconds ago
        )

        val remaining = session.getRemainingResendSeconds(10)
        // Should be around 7 seconds (10 - 3)
        assertTrue(remaining in 6..8)
    }

    @Test
    fun `session without forceResendingToken can be created`() {
        val session = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = null,
            sentAt = System.currentTimeMillis()
        )

        assertEquals("test-id", session.verificationId)
        assertEquals(null, session.forceResendingToken)
    }

    @Test
    fun `session equality works correctly`() {
        val session1 = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = null,
            sentAt = 1000000L
        )

        val session2 = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = null,
            sentAt = 1000000L
        )

        val session3 = SmsEnrollmentSession(
            verificationId = "different-id",
            phoneNumber = "+1234567890",
            forceResendingToken = null,
            sentAt = 1000000L
        )

        assertEquals(session1, session2)
        assertFalse(session1 == session3)
    }

    @Test
    fun `session copy works correctly`() {
        val original = SmsEnrollmentSession(
            verificationId = "test-id",
            phoneNumber = "+1234567890",
            forceResendingToken = null,
            sentAt = 1000000L
        )

        val copied = original.copy(verificationId = "new-id")

        assertEquals("new-id", copied.verificationId)
        assertEquals("+1234567890", copied.phoneNumber)
        assertEquals("test-id", original.verificationId) // Original unchanged
    }
}
