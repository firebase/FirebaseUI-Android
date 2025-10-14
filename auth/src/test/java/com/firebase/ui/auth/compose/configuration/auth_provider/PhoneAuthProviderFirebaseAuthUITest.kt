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

package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive unit tests for Phone Authentication provider methods in FirebaseAuthUI.
 *
 * Tests cover all phone auth methods:
 * - verifyPhoneNumber (instant verification, manual verification, resend)
 * - submitVerificationCode
 * - signInWithPhoneAuthCredential
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PhoneAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockPhoneAuthVerifier: AuthProvider.Phone.Verifier

    @Mock
    private lateinit var mockPhoneAuthCredentialProvider: AuthProvider.Phone.CredentialProvider

    private lateinit var firebaseApp: FirebaseApp
    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        FirebaseAuthUI.clearInstanceCache()

        applicationContext = ApplicationProvider.getApplicationContext()

        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        firebaseApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        try {
            firebaseApp.delete()
        } catch (_: Exception) {
            // Ignore if already deleted
        }
    }

    // =============================================================================================
    // verifyPhoneNumber Tests
    // =============================================================================================

    @Test
    fun `verifyPhoneNumber - instant verification succeeds and emits SMSAutoVerified`() = runTest {
        val mockCredential = mock(PhoneAuthCredential::class.java)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )

        `when`(
            mockPhoneAuthVerifier.verifyPhoneNumber(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                eq(true)
            )
        ).thenReturn(AuthProvider.Phone.VerifyPhoneNumberResult.AutoVerified(mockCredential))

        instance.verifyPhoneNumber(
            provider = phoneProvider,
            phoneNumber = "+1234567890",
            verifier = mockPhoneAuthVerifier
        )

        val finalState = instance.authStateFlow().first { it is AuthState.SMSAutoVerified }
        assertThat(finalState).isInstanceOf(AuthState.SMSAutoVerified::class.java)
        val autoVerifiedState = finalState as AuthState.SMSAutoVerified
        assertThat(autoVerifiedState.credential).isEqualTo(mockCredential)
    }

    @Test
    fun `verifyPhoneNumber - manual verification emits PhoneNumberVerificationRequired`() =
        runTest {
            val mockToken = mock(PhoneAuthProvider.ForceResendingToken::class.java)
            val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
            val phoneProvider = AuthProvider.Phone(
                defaultNumber = null,
                defaultCountryCode = null,
                allowedCountries = null,
                timeout = 60L,
                isInstantVerificationEnabled = true
            )

            `when`(
                mockPhoneAuthVerifier.verifyPhoneNumber(
                    any(),
                    any(),
                    any(),
                    anyOrNull(),
                    anyOrNull(),
                    eq(true)
                )
            ).thenReturn(
                AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                    "test-verification-id",
                    mockToken
                )
            )

            instance.verifyPhoneNumber(
                provider = phoneProvider,
                phoneNumber = "+1234567890",
                verifier = mockPhoneAuthVerifier
            )

            val finalState =
                instance.authStateFlow().first { it is AuthState.PhoneNumberVerificationRequired }
            assertThat(finalState).isInstanceOf(AuthState.PhoneNumberVerificationRequired::class.java)
            val verificationState = finalState as AuthState.PhoneNumberVerificationRequired
            assertThat(verificationState.verificationId).isEqualTo("test-verification-id")
            assertThat(verificationState.forceResendingToken).isEqualTo(mockToken)
        }

    @Test
    fun `verifyPhoneNumber - with forceResendingToken resends code`() = runTest {
        val mockToken = mock(PhoneAuthProvider.ForceResendingToken::class.java)
        val newMockToken = mock(PhoneAuthProvider.ForceResendingToken::class.java)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )

        `when`(
            mockPhoneAuthVerifier.verifyPhoneNumber(
                any(),
                any(),
                any(),
                eq(mockToken),
                anyOrNull(),
                eq(true)
            )
        ).thenReturn(
            AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                "new-verification-id",
                newMockToken
            )
        )

        instance.verifyPhoneNumber(
            provider = phoneProvider,
            phoneNumber = "+1234567890",
            forceResendingToken = mockToken,
            verifier = mockPhoneAuthVerifier
        )

        val finalState =
            instance.authStateFlow().first { it is AuthState.PhoneNumberVerificationRequired }
        assertThat(finalState).isInstanceOf(AuthState.PhoneNumberVerificationRequired::class.java)
        val verificationState = finalState as AuthState.PhoneNumberVerificationRequired
        assertThat(verificationState.verificationId).isEqualTo("new-verification-id")
        assertThat(verificationState.forceResendingToken).isEqualTo(newMockToken)
    }

    @Test
    fun `verifyPhoneNumber - respects isInstantVerificationEnabled flag`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = false // Disabled
        )

        `when`(
            mockPhoneAuthVerifier.verifyPhoneNumber(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                eq(false)
            )
        ).thenReturn(
            AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                "test-id",
                mock()
            )
        )

        instance.verifyPhoneNumber(
            provider = phoneProvider,
            phoneNumber = "+1234567890",
            verifier = mockPhoneAuthVerifier
        )

        verify(mockPhoneAuthVerifier).verifyPhoneNumber(
            any(),
            any(),
            any(),
            anyOrNull(),
            anyOrNull(),
            eq(false)
        )
    }

    // =============================================================================================
    // submitVerificationCode Tests
    // =============================================================================================

    @Test
    fun `submitVerificationCode - creates credential and signs in successfully`() = runTest {
        val mockCredential = mock(PhoneAuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        `when`(mockPhoneAuthCredentialProvider.getCredential("test-verification-id", "123456"))
            .thenReturn(mockCredential)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(phoneProvider)
            }
        }

        val result = instance.submitVerificationCode(
            config = config,
            verificationId = "test-verification-id",
            code = "123456",
            credentialProvider = mockPhoneAuthCredentialProvider
        )

        assertThat(result).isNotNull()
        assertThat(result?.user).isEqualTo(mockUser)
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)
    }

    // =============================================================================================
    // signInWithPhoneAuthCredential Tests
    // =============================================================================================

    @Test
    fun `signInWithPhoneAuthCredential - successful sign in with credential`() = runTest {
        val mockCredential = mock(PhoneAuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(phoneProvider)
            }
        }

        val result = instance.signInWithPhoneAuthCredential(
            config = config,
            credential = mockCredential
        )

        assertThat(result).isNotNull()
        assertThat(result?.user).isEqualTo(mockUser)
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)
    }

    @Test
    fun `signInWithPhoneAuthCredential - handles anonymous upgrade`() = runTest {
        val anonymousUser = mock(FirebaseUser::class.java)
        `when`(anonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(anonymousUser)

        val mockCredential = mock(PhoneAuthCredential::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(anonymousUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(anonymousUser.linkWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(phoneProvider)
            }
            isAnonymousUpgradeEnabled = true
        }

        val result = instance.signInWithPhoneAuthCredential(
            config = config,
            credential = mockCredential
        )

        assertThat(result).isNotNull()
        verify(anonymousUser).linkWithCredential(mockCredential)
    }

}