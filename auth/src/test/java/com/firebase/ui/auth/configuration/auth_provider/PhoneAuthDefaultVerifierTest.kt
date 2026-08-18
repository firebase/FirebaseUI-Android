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

import com.firebase.ui.auth.configuration.auth_provider.AuthProvider.Phone.VerifyPhoneNumberResult
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PhoneAuthDefaultVerifierTest {

    private val verifier = AuthProvider.Phone.DefaultVerifier()
    private val mockAuth = mock(FirebaseAuth::class.java)

    /**
     * Records everything one collection of the verifier's flow observed: every emission in order,
     * plus the exception that terminated it (if any).
     */
    private class Verification {
        val emissions = mutableListOf<VerifyPhoneNumberResult>()
        var terminal: Throwable? = null
        lateinit var callbacks: OnVerificationStateChangedCallbacks
        lateinit var job: Job
    }

    /**
     * Looks up the callbacks stashed inside [PhoneAuthOptions]. There's no public accessor -
     * only an obfuscated zero-arg method whose return type is
     * [PhoneAuthProvider.OnVerificationStateChangedCallbacks]. We locate it reflectively and
     * assert exactly one such method exists, so this test breaks loudly (rather than silently)
     * if a future SDK bump changes the obfuscated shape.
     */
    private fun extractCallbacks(options: PhoneAuthOptions): OnVerificationStateChangedCallbacks {
        val candidates = PhoneAuthOptions::class.java.declaredMethods.filter {
            it.parameterCount == 0 &&
                it.returnType == OnVerificationStateChangedCallbacks::class.java
        }
        check(candidates.size == 1) {
            "Expected exactly one zero-arg accessor returning " +
                "OnVerificationStateChangedCallbacks on PhoneAuthOptions, found " +
                "${candidates.size}: $candidates"
        }
        val method = candidates.single()
        method.isAccessible = true
        return method.invoke(options) as OnVerificationStateChangedCallbacks
    }

    // UNDISPATCHED so the callbackFlow builder runs (and registers with Firebase) before we return.
    private fun TestScope.startVerification(
        mockedStatic: MockedStatic<PhoneAuthProvider>
    ): Verification {
        val verification = Verification()
        verification.job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                verifier.verifyPhoneNumber(
                    auth = mockAuth,
                    activity = null,
                    phoneNumber = "+15555550123",
                    timeout = 60L,
                    forceResendingToken = null,
                    multiFactorSession = null,
                    isInstantVerificationEnabled = true
                ).collect { verification.emissions += it }
            } catch (e: FirebaseException) {
                verification.terminal = e
            }
        }

        val captor = ArgumentCaptor.forClass(PhoneAuthOptions::class.java)
        mockedStatic.verify { PhoneAuthProvider.verifyPhoneNumber(captor.capture()) }
        verification.callbacks = extractCallbacks(captor.value)

        return verification
    }

    private fun autoVerified(result: VerifyPhoneNumberResult): PhoneAuthCredential {
        assertThat(result).isInstanceOf(VerifyPhoneNumberResult.AutoVerified::class.java)
        return (result as VerifyPhoneNumberResult.AutoVerified).credential
    }

    private fun manual(
        result: VerifyPhoneNumberResult
    ): VerifyPhoneNumberResult.NeedsManualVerification {
        assertThat(result)
            .isInstanceOf(VerifyPhoneNumberResult.NeedsManualVerification::class.java)
        return result as VerifyPhoneNumberResult.NeedsManualVerification
    }

    // =============================================================================================
    // Single callbacks - one callback in, one emission out.
    // =============================================================================================

    @Test
    fun `onVerificationCompleted emits AutoVerified`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val credential = mock(PhoneAuthCredential::class.java)

                verification.callbacks.onVerificationCompleted(credential)
                runCurrent()

                assertThat(verification.emissions).hasSize(1)
                assertThat(autoVerified(verification.emissions[0])).isEqualTo(credential)
                assertThat(verification.terminal).isNull()
            }
        }

    @Test
    fun `onCodeSent emits NeedsManualVerification`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val token = mock(PhoneAuthProvider.ForceResendingToken::class.java)

                verification.callbacks.onCodeSent("verification-id", token)
                runCurrent()

                assertThat(verification.emissions).hasSize(1)
                val emitted = manual(verification.emissions[0])
                assertThat(emitted.verificationId).isEqualTo("verification-id")
                assertThat(emitted.token).isEqualTo(token)
                assertThat(verification.terminal).isNull()
            }
        }

    @Test
    fun `onVerificationFailed terminates the flow with the exception`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)

                verification.callbacks.onVerificationFailed(FirebaseException("boom"))
                runCurrent()

                assertThat(verification.emissions).isEmpty()
                // kotlinx.coroutines copies exceptions crossing a channel, so compare type and
                // message instead of reference identity.
                assertThat(verification.terminal).isInstanceOf(FirebaseException::class.java)
                assertThat(verification.terminal?.message).isEqualTo("boom")
                assertThat(verification.job.isActive).isFalse()
            }
        }

    // =============================================================================================
    // Multiple callbacks - every callback becomes an emission; none is dropped.
    // =============================================================================================

    @Test
    fun `onCodeSent then onVerificationCompleted both emit - regression for issue 2446`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val token = mock(PhoneAuthProvider.ForceResendingToken::class.java)
                val credential = mock(PhoneAuthCredential::class.java)

                // Firebase's SMS auto-retrieval order. Under the old single-shot continuation this
                // threw "IllegalStateException: Already resumed" (or silently dropped the
                // credential); a flow simply emits twice.
                val thrownAtCallbackSite = runCatching {
                    verification.callbacks.onCodeSent("verification-id", token)
                    verification.callbacks.onVerificationCompleted(credential)
                }.exceptionOrNull()
                runCurrent()

                assertThat(thrownAtCallbackSite).isNull()
                assertThat(verification.emissions).hasSize(2)
                val first = manual(verification.emissions[0])
                assertThat(first.verificationId).isEqualTo("verification-id")
                assertThat(first.token).isEqualTo(token)
                assertThat(autoVerified(verification.emissions[1])).isEqualTo(credential)
                assertThat(verification.terminal).isNull()
            }
        }

    @Test
    fun `onVerificationCompleted then onCodeSent both emit`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val credential = mock(PhoneAuthCredential::class.java)
                val token = mock(PhoneAuthProvider.ForceResendingToken::class.java)

                verification.callbacks.onVerificationCompleted(credential)
                verification.callbacks.onCodeSent("later-verification-id", token)
                runCurrent()

                assertThat(verification.emissions).hasSize(2)
                assertThat(autoVerified(verification.emissions[0])).isEqualTo(credential)
                val second = manual(verification.emissions[1])
                assertThat(second.verificationId).isEqualTo("later-verification-id")
                assertThat(second.token).isEqualTo(token)
                assertThat(verification.terminal).isNull()
            }
        }

    @Test
    fun `onCodeSent then onVerificationFailed emits then terminates`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val token = mock(PhoneAuthProvider.ForceResendingToken::class.java)

                verification.callbacks.onCodeSent("verification-id", token)
                verification.callbacks.onVerificationFailed(FirebaseException("later failure"))
                runCurrent()

                assertThat(verification.emissions).hasSize(1)
                assertThat(manual(verification.emissions[0]).verificationId)
                    .isEqualTo("verification-id")
                assertThat(verification.terminal).isInstanceOf(FirebaseException::class.java)
                assertThat(verification.terminal?.message).isEqualTo("later failure")
            }
        }

    @Test
    fun `onCodeSent then onCodeAutoRetrievalTimeOut emits then completes`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val token = mock(PhoneAuthProvider.ForceResendingToken::class.java)

                verification.callbacks.onCodeSent("verification-id", token)
                verification.callbacks.onCodeAutoRetrievalTimeOut("verification-id")
                runCurrent()

                assertThat(verification.emissions).hasSize(1)
                assertThat(manual(verification.emissions[0]).verificationId)
                    .isEqualTo("verification-id")
                // Firebase's own terminal, so the collector completes rather than hanging.
                assertThat(verification.terminal).isNull()
                assertThat(verification.job.isCompleted).isTrue()
                assertThat(verification.job.isCancelled).isFalse()
            }
        }

    @Test
    fun `duplicate onVerificationCompleted emits both credentials`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val firstCredential = mock(PhoneAuthCredential::class.java)
                val secondCredential = mock(PhoneAuthCredential::class.java)

                verification.callbacks.onVerificationCompleted(firstCredential)
                verification.callbacks.onVerificationCompleted(secondCredential)
                runCurrent()

                assertThat(verification.emissions).hasSize(2)
                assertThat(autoVerified(verification.emissions[0])).isEqualTo(firstCredential)
                assertThat(autoVerified(verification.emissions[1])).isEqualTo(secondCredential)
                assertThat(verification.terminal).isNull()
            }
        }

    @Test
    fun `duplicate onCodeSent emits both verification ids`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)
                val firstToken = mock(PhoneAuthProvider.ForceResendingToken::class.java)
                val secondToken = mock(PhoneAuthProvider.ForceResendingToken::class.java)

                verification.callbacks.onCodeSent("first-verification-id", firstToken)
                verification.callbacks.onCodeSent("second-verification-id", secondToken)
                runCurrent()

                assertThat(verification.emissions).hasSize(2)
                val first = manual(verification.emissions[0])
                assertThat(first.verificationId).isEqualTo("first-verification-id")
                assertThat(first.token).isEqualTo(firstToken)
                val second = manual(verification.emissions[1])
                assertThat(second.verificationId).isEqualTo("second-verification-id")
                assertThat(second.token).isEqualTo(secondToken)
                assertThat(verification.terminal).isNull()
            }
        }

    // =============================================================================================
    // Cancellation - callbacks arriving after the collector went away must be inert. `trySend` on
    // a closed channel returns a failed result instead of throwing, which is what replaces the
    // old first-callback-wins latch.
    // =============================================================================================

    @Test
    fun `callbacks after the collector is cancelled do not throw at the Firebase call site`() =
        runTest(UnconfinedTestDispatcher()) {
            mockStatic(PhoneAuthProvider::class.java).use { mockedStatic ->
                val verification = startVerification(mockedStatic)

                verification.job.cancel()
                runCurrent()
                assertThat(verification.job.isCancelled).isTrue()

                val thrownAtCallbackSite = runCatching {
                    verification.callbacks.onCodeSent(
                        "late-verification-id",
                        mock(PhoneAuthProvider.ForceResendingToken::class.java)
                    )
                    verification.callbacks.onVerificationCompleted(
                        mock(PhoneAuthCredential::class.java)
                    )
                    verification.callbacks.onVerificationFailed(FirebaseException("late failure"))
                }.exceptionOrNull()
                runCurrent()

                assertThat(thrownAtCallbackSite).isNull()
                assertThat(verification.emissions).isEmpty()
                assertThat(verification.terminal).isNull()
            }
        }
}
