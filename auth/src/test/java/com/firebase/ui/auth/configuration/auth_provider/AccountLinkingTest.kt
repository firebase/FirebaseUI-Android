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
import androidx.credentials.CredentialManager
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.PendingAccountLink
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.flowScope
import com.firebase.ui.auth.recordingScope
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.SignInMethodQueryResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The account-linking recovery's provider half: linking the credential that collided once the
 * user signs in to the account that owns the address, the lookup that names that account, and
 * the translated copy the collision now carries.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AccountLinkingTest {

    private lateinit var applicationContext: Context
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var stringProvider: DefaultAuthUIStringProvider

    /** The Facebook credential whose own sign-in hit the existing account. */
    private val pendingCredential = mock(AuthCredential::class.java)

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        firebaseApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        mockAuth = mock(FirebaseAuth::class.java)
        `when`(mockAuth.currentUser).thenReturn(null)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        try {
            firebaseApp.delete()
        } catch (_: Exception) {
        }
    }

    // =============================================================================================
    // linkPendingAccountCredential
    // =============================================================================================

    @Test
    fun `links the pending credential to the account with the same address and clears it`() =
        runTest {
            val (signIn, user) = signedInAs(OWNER_EMAIL)
            val linked = mock(AuthResult::class.java)
            `when`(user.linkWithCredential(pendingCredential)).thenReturn(Tasks.forResult(linked))
            val scope = scopeWithPendingLink()

            val result = scope.linkPendingAccountCredential(signIn)

            assertThat(result).isSameInstanceAs(linked)
            assertThat(scope.pendingAccountLink.value).isNull()
            verify(mockAuth, never()).signOut()
        }

    /** Addresses are case-insensitive, and Firebase does not normalise what the user typed. */
    @Test
    fun `an address that differs only in case still matches`() = runTest {
        val (signIn, user) = signedInAs(OWNER_EMAIL.uppercase())
        `when`(user.linkWithCredential(pendingCredential))
            .thenReturn(Tasks.forResult(mock(AuthResult::class.java)))

        scopeWithPendingLink().linkPendingAccountCredential(signIn)

        verify(user).linkWithCredential(pendingCredential)
    }

    /**
     * Credential Manager lets the user pick any Google account. Linking to one with a different
     * address would hand the credential to someone else, so the sign-in is undone and the linking
     * error raised again, still holding the credential, for the user to pick the right account.
     */
    @Test
    fun `a different account is signed back out and the linking error raised again`() = runTest {
        val (signIn, user) = signedInAs("someone-else@example.com")
        val scope = scopeWithPendingLink()

        val error = assertThrows(AuthException.AccountLinkingRequiredException::class.java) {
            kotlinx.coroutines.runBlocking { scope.linkPendingAccountCredential(signIn) }
        }

        assertThat(error.message).isEqualTo(stringProvider.errorUserMismatch)
        assertThat(error.email).isEqualTo(OWNER_EMAIL)
        assertThat(error.credential).isSameInstanceAs(pendingCredential)
        assertThat(scope.isPendingLinkMismatch(error)).isTrue()
        assertThat(scope.pendingAccountLink.value).isNotNull()
        verify(mockAuth).signOut()
        verify(user, never()).linkWithCredential(any())
    }

    /** A failed link must not leave the host signed in to an account missing the credential. */
    @Test
    fun `a link that fails signs back out and reports the failure`() = runTest {
        val (signIn, user) = signedInAs(OWNER_EMAIL)
        `when`(user.linkWithCredential(pendingCredential))
            .thenReturn(Tasks.forException(FirebaseNetworkException("offline")))
        val scope = scopeWithPendingLink()

        assertThrows(AuthException.NetworkException::class.java) {
            kotlinx.coroutines.runBlocking { scope.linkPendingAccountCredential(signIn) }
        }

        assertThat(scope.pendingAccountLink.value).isNull()
        verify(mockAuth).signOut()
    }

    /**
     * Firebase can report a link failure as a collision. Raising the linking error for it would
     * send the user straight back round the recovery that just failed.
     */
    @Test
    fun `a link that fails as a collision is not offered the linking recovery again`() = runTest {
        val (signIn, user) = signedInAs(OWNER_EMAIL)
        `when`(user.linkWithCredential(pendingCredential)).thenReturn(
            Tasks.forException(
                FirebaseAuthUserCollisionException(
                    "ERROR_CREDENTIAL_ALREADY_IN_USE",
                    "already linked to another user"
                )
            )
        )
        val scope = scopeWithPendingLink()

        val error = assertThrows(AuthException.UnknownException::class.java) {
            kotlinx.coroutines.runBlocking { scope.linkPendingAccountCredential(signIn) }
        }

        assertThat(error.message).isEqualTo(stringProvider.unknownErrorRecoveryMessage)
        verify(mockAuth).signOut()
    }

    @Test
    fun `nothing pending leaves the sign-in as it was`() = runTest {
        val (signIn, user) = signedInAs(OWNER_EMAIL)
        val scope = FirebaseAuthUI.create(firebaseApp, mockAuth).recordingScope(config(), mutableListOf())

        assertThat(scope.linkPendingAccountCredential(signIn)).isSameInstanceAs(signIn)
        verify(user, never()).linkWithCredential(any())
    }

    /** A reauthentication proves who the user is; it is never the account-linking recovery. */
    @Test
    fun `a reauthentication never links`() = runTest {
        val (signIn, user) = signedInAs(OWNER_EMAIL)
        val scope = scopeWithPendingLink(config(isReauthenticationMode = true))

        assertThat(scope.linkPendingAccountCredential(signIn)).isSameInstanceAs(signIn)
        verify(user, never()).linkWithCredential(any())
    }

    // =============================================================================================
    // The provider flows link before reporting success
    // =============================================================================================

    /**
     * The point of linking inside the provider: the host is never handed the unlinked result. The
     * Success the flow reports already carries the link's result.
     */
    @Test
    fun `a Google sign-in during the recovery links before it reports success`() = runTest {
        val googleCredential = mock(AuthCredential::class.java)
        val credentialManager = mock(AuthProvider.Google.CredentialManagerProvider::class.java)
        `when`(
            credentialManager.getGoogleCredential(
                context = eq(applicationContext),
                credentialManager = any<CredentialManager>(),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(
            AuthProvider.Google.GoogleSignInResult(
                credential = googleCredential,
                idToken = "test-id-token",
                displayName = null,
                photoUrl = null,
            )
        )
        val (signIn, user) = signedInAs(OWNER_EMAIL)
        `when`(mockAuth.signInWithCredential(googleCredential)).thenReturn(Tasks.forResult(signIn))
        val linked = mock(AuthResult::class.java)
        `when`(linked.user).thenReturn(user)
        `when`(user.linkWithCredential(pendingCredential)).thenReturn(Tasks.forResult(linked))
        val instance = FirebaseAuthUI.create(firebaseApp, mockAuth)
        val scope = instance.flowScope(config())
        scope.pendingAccountLink.value = PendingAccountLink(pendingCredential, OWNER_EMAIL)

        scope.signInWithGoogle(
            context = applicationContext,
            provider = googleProvider,
            credentialManagerProvider = credentialManager,
        )

        verify(user).linkWithCredential(pendingCredential)
        val finalState = instance.authStateFlow().first { it !is AuthState.Loading }
        assertThat(finalState).isEqualTo(
            AuthState.Success(result = linked, user = user, isNewUser = false)
        )
    }

    /** OAuth providers sign in through Firebase's own activity flow, not the shared credential path. */
    @Test
    fun `an OAuth sign-in during the recovery links before it reports success`() = runTest {
        val (signIn, user) = signedInAs(OWNER_EMAIL)
        `when`(signIn.credential).thenReturn(mock(OAuthCredential::class.java))
        `when`(mockAuth.pendingAuthResult).thenReturn(null)
        `when`(mockAuth.startActivityForSignInWithProvider(any<Activity>(), any<OAuthProvider>()))
            .thenReturn(Tasks.forResult(signIn))
        val linked = mock(AuthResult::class.java)
        `when`(linked.user).thenReturn(user)
        `when`(user.linkWithCredential(pendingCredential)).thenReturn(Tasks.forResult(linked))
        val github = AuthProvider.Github(customParameters = emptyMap())
        val instance = FirebaseAuthUI.create(firebaseApp, mockAuth)
        val scope = instance.flowScope(config(github))
        scope.pendingAccountLink.value = PendingAccountLink(pendingCredential, OWNER_EMAIL)

        scope.signInWithProvider(applicationContext, mock(Activity::class.java), github)

        verify(user).linkWithCredential(pendingCredential)
        val finalState = instance.authStateFlow().first { it !is AuthState.Loading }
        assertThat(finalState).isEqualTo(
            AuthState.Success(result = linked, user = user, isNewUser = false)
        )
    }

    // =============================================================================================
    // findAccountOwnerSignInMethod
    // =============================================================================================

    /** Off by default: with email enumeration protection on, the lookup reveals nothing. */
    @Test
    fun `the owner lookup does nothing unless the legacy lookup is enabled`() = runTest {
        val scope = FirebaseAuthUI.create(firebaseApp, mockAuth).flowScope(config())

        assertThat(scope.findAccountOwnerSignInMethod(OWNER_EMAIL, listOf(googleProvider))).isNull()
        verify(mockAuth, never()).fetchSignInMethodsForEmail(any())
    }

    /**
     * The existing different-method lookup skips a password account; for linking it is exactly
     * the account to sign back in to.
     */
    @Test
    fun `the owner lookup names a password account`() = runTest {
        signInMethodsAre(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)
        val scope = FirebaseAuthUI.create(firebaseApp, mockAuth).flowScope(config(legacyLookup = true))

        assertThat(scope.findAccountOwnerSignInMethod(OWNER_EMAIL, listOf(emailProvider, googleProvider)))
            .isEqualTo(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)
    }

    @Test
    fun `the owner lookup names a configured provider`() = runTest {
        signInMethodsAre(GoogleAuthProvider.PROVIDER_ID)
        val scope = FirebaseAuthUI.create(firebaseApp, mockAuth).flowScope(config(legacyLookup = true))

        assertThat(scope.findAccountOwnerSignInMethod(OWNER_EMAIL, listOf(emailProvider, googleProvider)))
            .isEqualTo(GoogleAuthProvider.PROVIDER_ID)
    }

    @Test
    fun `the owner lookup skips what the recovery cannot open`() = runTest {
        // Email-link sign-in is not enabled, and Apple is not among the candidates.
        signInMethodsAre(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD, "apple.com")
        val scope = FirebaseAuthUI.create(firebaseApp, mockAuth).flowScope(config(legacyLookup = true))

        assertThat(scope.findAccountOwnerSignInMethod(OWNER_EMAIL, listOf(emailProvider, googleProvider)))
            .isNull()
    }

    // =============================================================================================
    // Translated copy (Part 3)
    // =============================================================================================

    /** The dialog shows the exception's message, so a hard-coded one is never translated. */
    @Test
    fun `a provider collision carries the translated linking message`() = runTest {
        val collision = mock(FirebaseAuthUserCollisionException::class.java)
        `when`(collision.email).thenReturn(OWNER_EMAIL)
        `when`(mockAuth.signInWithCredential(pendingCredential))
            .thenReturn(Tasks.forException(collision))
        val scope = FirebaseAuthUI.create(firebaseApp, mockAuth).recordingScope(config(), mutableListOf())

        val error = assertThrows(AuthException.AccountLinkingRequiredException::class.java) {
            kotlinx.coroutines.runBlocking {
                scope.signInAndLinkWithCredential(pendingCredential, AuthProvider.Facebook())
            }
        }

        assertThat(error.message).isEqualTo(stringProvider.accountLinkingRequiredRecoveryMessage)
        assertThat(error.email).isEqualTo(OWNER_EMAIL)
        assertThat(error.credential).isSameInstanceAs(pendingCredential)
    }

    // =============================================================================================
    // Helpers
    // =============================================================================================

    private val emailProvider = AuthProvider.Email(
        emailLinkActionCodeSettings = null,
        passwordValidationRules = emptyList(),
    )

    private val googleProvider = AuthProvider.Google(
        scopes = emptyList(),
        serverClientId = "test-client-id",
    )

    private fun config(
        vararg extra: AuthProvider,
        legacyLookup: Boolean = false,
        isReauthenticationMode: Boolean = false,
    ): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(emailProvider)
            provider(googleProvider)
            extra.forEach { provider(it) }
        }
        legacyFetchSignInWithEmail = legacyLookup
        this.isReauthenticationMode = isReauthenticationMode
    }

    private fun scopeWithPendingLink(configuration: AuthUIConfiguration = config()) =
        FirebaseAuthUI.create(firebaseApp, mockAuth)
            .recordingScope(configuration, mutableListOf())
            .also { it.pendingAccountLink.value = PendingAccountLink(pendingCredential, OWNER_EMAIL) }

    private fun signedInAs(email: String): Pair<AuthResult, FirebaseUser> {
        val user = mock(FirebaseUser::class.java)
        `when`(user.email).thenReturn(email)
        `when`(user.uid).thenReturn("owner-uid")
        val result = mock(AuthResult::class.java)
        `when`(result.user).thenReturn(user)
        return result to user
    }

    private fun signInMethodsAre(vararg methods: String) {
        val query = mock(SignInMethodQueryResult::class.java)
        `when`(query.signInMethods).thenReturn(methods.toList())
        `when`(mockAuth.fetchSignInMethodsForEmail(OWNER_EMAIL)).thenReturn(Tasks.forResult(query))
    }

    private companion object {
        const val OWNER_EMAIL = "owner@example.com"
    }
}
