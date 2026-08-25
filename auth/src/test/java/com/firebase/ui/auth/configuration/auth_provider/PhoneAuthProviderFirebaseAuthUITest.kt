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

package com.firebase.ui.auth.configuration.auth_provider

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.timeout
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
    private lateinit var phoneConfig: AuthUIConfiguration

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

        phoneConfig = authUIConfiguration {
            context = applicationContext
            providers {
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }
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
                auth = any(),
                activity = anyOrNull(),
                phoneNumber = any(),
                timeout =  eq(60L),
                forceResendingToken = anyOrNull(),
                multiFactorSession = anyOrNull(),
                isInstantVerificationEnabled = eq(true)
            )
        ).thenReturn(
            flowOf(AuthProvider.Phone.VerifyPhoneNumberResult.AutoVerified(mockCredential))
        )

        instance.verifyPhoneNumber(
            provider = phoneProvider,
            activity = null,
            phoneNumber = "+1234567890",
            config = phoneConfig,
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
                    auth = any(),
                    activity = anyOrNull(),
                    phoneNumber = any(),
                    timeout = eq(60L),
                    forceResendingToken = anyOrNull(),
                    multiFactorSession = anyOrNull(),
                    isInstantVerificationEnabled = eq(true)
                )
            ).thenReturn(
                flowOf(
                    AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                        "test-verification-id",
                        mockToken
                    )
                )
            )

            instance.verifyPhoneNumber(
                provider = phoneProvider,
                activity = null,
                phoneNumber = "+1234567890",
                config = phoneConfig,
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
    fun `verifyPhoneNumber - late auto-verification after code sent emits SMSAutoVerified`() =
        runTest {
            val mockToken = mock(PhoneAuthProvider.ForceResendingToken::class.java)
            val mockCredential = mock(PhoneAuthCredential::class.java)
            val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
            val phoneProvider = AuthProvider.Phone(
                defaultNumber = null,
                defaultCountryCode = null,
                allowedCountries = null,
                timeout = 60L,
                isInstantVerificationEnabled = true
            )

            // Firebase's SMS auto-retrieval order: the code is sent first, then the credential
            // arrives on its own. The late credential must not be dropped.
            `when`(
                mockPhoneAuthVerifier.verifyPhoneNumber(
                    auth = any(),
                    activity = anyOrNull(),
                    phoneNumber = any(),
                    timeout = eq(60L),
                    forceResendingToken = anyOrNull(),
                    multiFactorSession = anyOrNull(),
                    isInstantVerificationEnabled = eq(true)
                )
            ).thenReturn(
                flowOf(
                    AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                        "test-verification-id",
                        mockToken
                    ),
                    AuthProvider.Phone.VerifyPhoneNumberResult.AutoVerified(mockCredential)
                )
            )

            instance.verifyPhoneNumber(
                provider = phoneProvider,
                activity = null,
                phoneNumber = "+1234567890",
                config = phoneConfig,
                verifier = mockPhoneAuthVerifier
            )

            val finalState = instance.authStateFlow().first()
            assertThat(finalState).isInstanceOf(AuthState.SMSAutoVerified::class.java)
            assertThat((finalState as AuthState.SMSAutoVerified).credential)
                .isEqualTo(mockCredential)
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
                auth = any(),
                activity = anyOrNull(),
                phoneNumber = any(),
                timeout = eq(60L),
                forceResendingToken = eq(mockToken),
                multiFactorSession = anyOrNull(),
                isInstantVerificationEnabled = eq(true)
            )
        ).thenReturn(
            flowOf(
                AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                    "new-verification-id",
                    newMockToken
                )
            )
        )

        instance.verifyPhoneNumber(
            provider = phoneProvider,
            activity = null,
            phoneNumber = "+1234567890",
            config = phoneConfig,
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
                auth = any(),
                activity = anyOrNull(),
                phoneNumber = any(),
                timeout = eq(60L),
                forceResendingToken = anyOrNull(),
                multiFactorSession = anyOrNull(),
                isInstantVerificationEnabled = eq(false)
            )
        ).thenReturn(
            flowOf(
                AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                    "test-id",
                    mock()
                )
            )
        )

        instance.verifyPhoneNumber(
            provider = phoneProvider,
            activity = null,
            phoneNumber = "+1234567890",
            config = phoneConfig,
            verifier = mockPhoneAuthVerifier
        )

        verify(mockPhoneAuthVerifier).verifyPhoneNumber(
            any(),
            anyOrNull(),
            any(),
            eq(60L),
            anyOrNull(),
            anyOrNull(),
            eq(false)
        )
    }

    @Test
    fun `verifyPhoneNumber - cancellation propagates CancellationException and emits no Error`() =
        runTest {
            val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
            val phoneProvider = AuthProvider.Phone(
                defaultNumber = null,
                defaultCountryCode = null,
                allowedCountries = null,
                timeout = 60L,
                isInstantVerificationEnabled = true
            )
            val cancellingVerifier = object : AuthProvider.Phone.Verifier {
                override fun verifyPhoneNumber(
                    auth: FirebaseAuth,
                    activity: Activity?,
                    phoneNumber: String,
                    timeout: Long,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
                    multiFactorSession: MultiFactorSession?,
                    isInstantVerificationEnabled: Boolean,
                ): Flow<AuthProvider.Phone.VerifyPhoneNumberResult> = flow {
                    throw CancellationException("Verification cancelled")
                }
            }

            var thrown: Throwable? = null
            try {
                instance.verifyPhoneNumber(
                    provider = phoneProvider,
                    activity = null,
                    phoneNumber = "+1234567890",
                    config = phoneConfig,
                    verifier = cancellingVerifier
                )
            } catch (t: Throwable) {
                thrown = t
            }

            // The screen cancels this collection as routine bookkeeping, so the cancellation must
            // travel back untranslated and leave nothing behind in authStateFlow.
            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            assertThat(thrown).isNotInstanceOf(AuthException::class.java)
            assertThat(instance.authStateFlow().first())
                .isNotInstanceOf(AuthState.Error::class.java)
        }

    @Test
    fun `verifyPhoneNumber - cancellation does not clobber a newer unrelated state`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val deferred = startNeverResolvingVerifyPhoneNumber(instance)

        // A newer, unrelated state lands while the verification is still in flight.
        instance.updateAuthState(AuthState.PasswordResetLinkSent())

        deferred.cancel()
        try {
            deferred.await()
        } catch (_: CancellationException) {
            // Expected
        }

        val state = instance.authStateFlow().first()
        assertThat(state).isInstanceOf(AuthState.PasswordResetLinkSent::class.java)
    }

    @Test
    fun `verifyPhoneNumber - cancellation does not clear a newer Loading from a resend`() =
        runTest {
            val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
            val first = startNeverResolvingVerifyPhoneNumber(instance)

            // A resend starts while the first call is still in flight and emits its own Loading,
            // which carries the same message and so compares equal to the first one.
            val resend = startNeverResolvingVerifyPhoneNumber(instance)

            first.cancel()
            try {
                first.await()
            } catch (_: CancellationException) {
                // Expected
            }

            val state = instance.authStateFlow().first()
            assertThat(state).isInstanceOf(AuthState.Loading::class.java)

            resend.cancel()
        }

    // Starts verifyPhoneNumber against a flow that never emits, UNDISPATCHED so the call reaches
    // its suspension point (past the Loading emission) before the caller can cancel it.
    private fun CoroutineScope.startNeverResolvingVerifyPhoneNumber(
        instance: FirebaseAuthUI,
    ): Deferred<Unit> {
        val phoneProvider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )
        val neverResolvingVerifier = object : AuthProvider.Phone.Verifier {
            override fun verifyPhoneNumber(
                auth: FirebaseAuth,
                activity: Activity?,
                phoneNumber: String,
                timeout: Long,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
                multiFactorSession: MultiFactorSession?,
                isInstantVerificationEnabled: Boolean,
            ): Flow<AuthProvider.Phone.VerifyPhoneNumberResult> = flow { awaitCancellation() }
        }

        return async(start = CoroutineStart.UNDISPATCHED) {
            instance.verifyPhoneNumber(
                provider = phoneProvider,
                activity = null,
                phoneNumber = "+1234567890",
                config = phoneConfig,
                verifier = neverResolvingVerifier
            )
        }
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
            applicationContext,
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
            applicationContext,
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
            applicationContext,
            config = config,
            credential = mockCredential
        )

        assertThat(result).isNotNull()
        verify(anonymousUser).linkWithCredential(mockCredential)
    }

}