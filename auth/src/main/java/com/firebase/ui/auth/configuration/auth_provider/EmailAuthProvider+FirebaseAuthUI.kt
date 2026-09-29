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

import android.content.Context
import android.net.Uri
import android.util.Log
import com.firebase.ui.auth.R
import com.firebase.ui.auth.AuthFlowScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider.Companion.canLinkCredential
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider.Companion.canUpgradeAnonymous
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider.Companion.mergeProfile
import com.firebase.ui.auth.credentialmanager.PasswordCredentialCancelledException
import com.firebase.ui.auth.credentialmanager.PasswordCredentialException
import com.firebase.ui.auth.credentialmanager.PasswordCredentialHandler
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.EmailLinkParser
import com.firebase.ui.auth.util.PersistenceManager
import com.firebase.ui.auth.util.SessionUtils
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.SignInMethodQueryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val TAG = "EmailAuthProvider"

/**
 * Signs in or reauthenticates with [credential] depending on [AuthUIConfiguration.isReauthenticationMode].
 *
 * - Normal mode: [com.google.firebase.auth.FirebaseAuth.signInWithCredential], returns [AuthResult].
 * - Reauth mode: [com.google.firebase.auth.FirebaseUser.reauthenticate] (Task<Void>), returns null.
 *   Callers must reconstruct auth state from [com.google.firebase.auth.FirebaseAuth.currentUser].
 */
internal suspend fun AuthFlowScope.signInOrReauth(
    credential: AuthCredential,
): AuthResult? = if (config.isReauthenticationMode) {
    val currentUser = auth.currentUser
        ?: throw AuthException.UserNotFoundException(message = "No user is currently signed in for reauthentication")
    currentUser.reauthenticate(credential).await()
    null
} else {
    auth.signInWithCredential(credential).await()
}

/**
 * Creates an email/password account, or links the credential to the signed-in anonymous user.
 *
 * Validates the password against [AuthProvider.Email.minimumPasswordLength] and
 * [AuthProvider.Email.passwordValidationRules], refuses a new account when the provider
 * disallows one, then either links to the anonymous user or creates a fresh account and merges
 * [name] into the profile.
 *
 * @throws AuthException.AccountLinkingRequiredException when an anonymous upgrade meets an
 * account that already owns [email] — the host must sign that account in to link.
 */
internal suspend fun AuthFlowScope.createOrLinkUserWithEmailAndPassword(
    context: Context,
    provider: AuthProvider.Email,
    name: String?,
    email: String,
    password: String,
    credentialProvider: AuthProvider.Email.CredentialProvider = AuthProvider.Email.DefaultCredentialProvider(),
): AuthResult? {
    val canUpgrade = canUpgradeAnonymous(config, auth)
    val canLink = canLinkCredential(config, auth)
    val shouldLinkCredential = canUpgrade || canLink
    val pendingCredential =
        if (shouldLinkCredential) credentialProvider.getCredential(email, password) else null

    try {
        if (config.isReauthenticationMode) {
            throw AuthException.UnknownException(
                message = context.getString(R.string.fui_error_reauth_sign_up_not_allowed)
            )
        }
        if (!shouldLinkCredential &&
            (!provider.isNewAccountsAllowed || !config.isNewEmailAccountsAllowed)
        ) {
            throw AuthException.UserNotFoundException(
                message = context.getString(R.string.fui_error_email_does_not_exist)
            )
        }

        // Validate minimum password length
        if (password.length < provider.minimumPasswordLength) {
            throw AuthException.InvalidCredentialsException(
                message = context.getString(R.string.fui_error_password_too_short)
                    .format(provider.minimumPasswordLength)
            )
        }

        // Validate password against custom rules
        for (rule in provider.passwordValidationRules) {
            if (!rule.isValid(password)) {
                throw AuthException.WeakPasswordException(
                    message = rule.getErrorMessage(config.stringProvider),
                    reason = "Password does not meet custom validation rules"
                )
            }
        }

        emit(AuthState.Loading(config.stringProvider.loadingCreatingUser))
        val result = if (shouldLinkCredential) {
            auth.currentUser?.linkWithCredential(requireNotNull(pendingCredential))?.await()
        } else {
            auth.createUserWithEmailAndPassword(email, password).await()
        }.also { authResult ->
            authResult?.user?.let {
                // Merge display name into profile (photoUri is always null for email/password)
                mergeProfile(auth, name, null)
            }
        }

        // Save credentials to Credential Manager if enabled
        if (config.isCredentialManagerEnabled) {
            try {
                val credentialHandler = PasswordCredentialHandler(context)
                credentialHandler.savePassword(email, password)
                Log.d(TAG, "Password credential saved successfully for: $email")
            } catch (e: PasswordCredentialCancelledException) {
                // User cancelled - this is fine, don't break the auth flow
                Log.d(TAG, "User cancelled credential save for: $email")
            } catch (e: PasswordCredentialException) {
                // Failed to save - log but don't break the auth flow
                Log.w(TAG, "Failed to save password credential for: $email", e)
            }
        }

        // Save sign-in preference for "Continue as..." feature
        if (result != null) {
            try {
                SignInPreferenceManager.saveLastSignIn(
                    context = context,
                    providerId = "password",
                    identifier = email
                )
                Log.d(TAG, "Sign-in preference saved for: $email")
            } catch (e: Exception) {
                // Failed to save preference - log but don't break auth flow
                Log.w(TAG, "Failed to save sign-in preference for: $email", e)
            }
        }

        emitResult(result, defaultIsNewUser = true)
        return result
    } catch (e: FirebaseAuthUserCollisionException) {
        // Account collision: email already exists
        val accountLinkingException = AuthException.AccountLinkingRequiredException(
            message = "An account already exists with this email. " +
                    "Please sign in with your existing account.",
            email = e.email ?: email,
            credential = when {
                canUpgrade -> e.updatedCredential ?: pendingCredential
                canLink -> pendingCredential
                else -> null
            },
            cause = e
        )
        emit(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Create or link user with email and password was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Signs in with email and password, optionally linking a social credential afterwards.
 *
 * An anonymous upgrade never signs the anonymous user out: the credentials are validated in a
 * scratch auth instance first, and only then is [AuthException.AccountLinkingRequiredException]
 * thrown for the host to resolve. A normal sign-in links [credentialForLinking], when given,
 * and merges its profile.
 */
internal suspend fun AuthFlowScope.signInWithEmailAndPassword(
    context: Context,
    email: String,
    password: String,
    credentialForLinking: AuthCredential? = null,
    skipCredentialSave: Boolean = false,
): AuthResult? {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSigningIn))
        // In reauth mode build a credential and go through signInAndLinkWithCredential so
        // signInOrReauth routes to FirebaseUser.reauthenticate() instead of signInWithCredential().
        if (config.isReauthenticationMode) {
            return signInAndLinkWithCredential(
                credential = EmailAuthProvider.getCredential(email, password),
            )
        }
        return if (canUpgradeAnonymous(config, auth)) {
            // Anonymous upgrade flow: validate credential in scratch auth
            val credentialToValidate = EmailAuthProvider.getCredential(email, password)

            // Check if we're linking a social provider credential
            val isSocialProvider = credentialForLinking != null &&
                    (Provider.fromId(credentialForLinking.provider)?.isSocialProvider ?: false)

            // Create scratch auth instance to avoid losing anonymous user state
            val appExplicitlyForValidation = FirebaseApp.initializeApp(
                context,
                auth.app.options,
                "FUIAuthScratchApp_${System.currentTimeMillis()}"
            )
            val authExplicitlyForValidation = FirebaseAuth
                .getInstance(appExplicitlyForValidation)

            if (isSocialProvider) {
                // Safe link: sign in with email, then link social credential
                authExplicitlyForValidation
                    .signInWithCredential(credentialToValidate).await()
                    .user?.linkWithCredential(credentialForLinking)?.await()
                    .also {
                        // Throw AccountLinkingRequiredException after successful validation
                        val accountLinkingException = AuthException.AccountLinkingRequiredException(
                            message = "An account already exists with this email. " +
                                    "Please sign in with your existing account to upgrade your anonymous account.",
                            email = email,
                            credential = credentialToValidate,
                            cause = null
                        )
                        emit(AuthState.Error(accountLinkingException))
                        throw accountLinkingException
                    }
            } else {
                // Just validate the email credential
                // No linking for non-federated IDPs
                authExplicitlyForValidation
                    .signInWithCredential(credentialToValidate).await()
                    .also {
                        // Throw AccountLinkingRequiredException after successful validation
                        // Account exists and user is anonymous - needs to link accounts
                        val accountLinkingException = AuthException.AccountLinkingRequiredException(
                            message = "An account already exists with this email. " +
                                    "Please sign in with your existing account to upgrade your anonymous account.",
                            email = email,
                            credential = credentialToValidate,
                            cause = null
                        )
                        emit(AuthState.Error(accountLinkingException))
                        throw accountLinkingException
                    }
            }
        } else {
            // Normal sign-in
            auth.signInWithEmailAndPassword(email, password).await()
                .let { result ->
                    // If there's a credential to link, link it after sign-in
                    if (credentialForLinking != null) {
                        val linkResult = result.user
                            ?.linkWithCredential(credentialForLinking)
                            ?.await()

                        // Merge profile from social provider
                        linkResult?.user?.let { user ->
                            mergeProfile(
                                auth,
                                user.displayName,
                                user.photoUrl
                            )
                        }

                        linkResult ?: result
                    } else {
                        result
                    }
                }
        }.also { result ->
            // Save credentials to Credential Manager if enabled
            // Skip if user signed in with a retrieved credential (already saved)
            if (config.isCredentialManagerEnabled && result != null && !skipCredentialSave) {
                try {
                    val credentialHandler = PasswordCredentialHandler(context)
                    credentialHandler.savePassword(email, password)
                    Log.d(TAG, "Password credential saved successfully for: $email")
                } catch (e: PasswordCredentialCancelledException) {
                    // User cancelled - this is fine, don't break the auth flow
                    Log.d(TAG, "User cancelled credential save for: $email")
                } catch (e: PasswordCredentialException) {
                    // Failed to save - log but don't break the auth flow
                    Log.w(TAG, "Failed to save password credential for: $email", e)
                }
            }

            // Save sign-in preference for "Continue as..." feature
            if (result != null) {
                try {
                    SignInPreferenceManager.saveLastSignIn(
                        context = context,
                        providerId = "password",
                        identifier = email
                    )
                    Log.d(TAG, "Sign-in preference saved for: $email")
                } catch (e: Exception) {
                    // Failed to save preference - log but don't break auth flow
                    Log.w(TAG, "Failed to save sign-in preference for: $email", e)
                }
            }

            emitResult(result)
        }
    } catch (e: FirebaseAuthMultiFactorException) {
        // MFA required - extract resolver and update state
        val resolver = e.resolver
        val hint = resolver.hints.firstOrNull()?.displayName
        emit(AuthState.RequiresMfa(resolver, hint))
        return null
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with email and password was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = recoverLegacyDifferentSignInMethod(email, e)
            ?: AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}

private suspend fun AuthFlowScope.recoverLegacyDifferentSignInMethod(
    email: String,
    cause: Exception,
): AuthException.DifferentSignInMethodRequiredException? {
    if (!config.legacyFetchSignInWithEmail) {
        return null
    }

    val authException = AuthException.from(cause, config.stringProvider)
    if (authException !is AuthException.InvalidCredentialsException &&
        authException !is AuthException.UserNotFoundException) {
        return null
    }

    val signInMethods = fetchLegacySignInMethods(email)
    val suggestedSignInMethod = selectSuggestedLegacySignInMethod(config, signInMethods) ?: return null

    return AuthException.DifferentSignInMethodRequiredException(
        message = config.stringProvider.accountLinkingRequiredRecoveryMessage,
        email = email,
        signInMethods = signInMethods,
        suggestedSignInMethod = suggestedSignInMethod,
        cause = cause
    )
}

private fun selectSuggestedLegacySignInMethod(
    config: AuthUIConfiguration,
    signInMethods: List<String>,
): String? {
    if (signInMethods.isEmpty() ||
        EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD in signInMethods) {
        return null
    }

    val emailProvider = config.providers.filterIsInstance<AuthProvider.Email>().firstOrNull()
    val configuredProviderIds = config.providers.map { it.providerId }.toSet()

    return signInMethods.firstOrNull { signInMethod ->
        when {
            signInMethod == EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD -> {
                emailProvider?.isEmailLinkSignInEnabled == true
            }

            signInMethod == EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD -> false
            else -> signInMethod in configuredProviderIds
        }
    }
}

private suspend fun AuthFlowScope.fetchLegacySignInMethods(email: String): List<String> {
    return try {
        @Suppress("DEPRECATION")
        auth.fetchSignInMethodsForEmail(email)
            .await()
            .toSignInMethods()
    } catch (fetchException: Exception) {
        Log.w(TAG, "Legacy fetchSignInMethodsForEmail failed for: $email", fetchException)
        emptyList()
    }
}

private fun SignInMethodQueryResult?.toSignInMethods(): List<String> =
    this?.signInMethods?.filter { it.isNotBlank() } ?: emptyList()

/**
 * Signs in with [credential], or links it to the signed-in anonymous user when upgrade is on.
 *
 * Merges [displayName] and [photoUrl] into the Firebase profile once authenticated. A collision
 * surfaces as [AuthException.AccountLinkingRequiredException] rather than the raw Firebase
 * exception, so the host can drive the linking flow.
 */
internal suspend fun AuthFlowScope.signInAndLinkWithCredential(
    credential: AuthCredential,
    provider: AuthProvider? = null,
    displayName: String? = null,
    photoUrl: Uri? = null,
): AuthResult? {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingLinkingCredential))
        val result = if (canUpgradeAnonymous(config, auth) || canLinkCredential(config, auth)) {
            auth.currentUser?.linkWithCredential(credential)?.await()
        } else {
            signInOrReauth(credential)
        }
        // signInOrReauth returns null in reauth mode (Task<Void> has no AuthResult).
        // Reconstruct success state from the now-reauthenticated current user.
        if (result == null && config.isReauthenticationMode) {
            val reauthenticatedUser = auth.currentUser
                ?: throw AuthException.UserNotFoundException(
                    message = "No user is currently signed in for reauthentication"
                )
            emit(
                AuthState.Success(
                    result = null,
                    user = reauthenticatedUser,
                    reauthenticatedUid = reauthenticatedUser.uid,
                )
            )
            return null
        }
        result?.user?.let { mergeProfile(auth, displayName, photoUrl) }
        emitResult(result)
        return result
    } catch (e: FirebaseAuthMultiFactorException) {
        // MFA required - extract resolver and update state
        val resolver = e.resolver
        val hint = resolver.hints.firstOrNull()?.displayName
        emit(AuthState.RequiresMfa(resolver, hint))
        return null
    } catch (e: FirebaseAuthUserCollisionException) {
        // Account collision: account already exists with different sign-in method
        // Create AccountLinkingRequiredException with credential for linking
        val email = e.email
        val credentialForException = if (canUpgradeAnonymous(config, auth)) {
            // For anonymous upgrade, use the updated credential from the exception
            e.updatedCredential ?: credential
        } else {
            // For non-anonymous, use the original credential
            credential
        }

        val accountLinkingException = AuthException.AccountLinkingRequiredException(
            message = "An account already exists with the email ${email ?: ""}. " +
                    "Please sign in with your existing account to link " +
                    "your ${provider?.providerName ?: "this provider"} account.",
            email = email,
            credential = credentialForException,
            cause = e
        )
        emit(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in and link with credential was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Sends a passwordless sign-in link to [email].
 *
 * The link's continue URL carries a session id, the anonymous user's id when upgrading, and the
 * force-same-device flag; the email and session are persisted so [signInWithEmailLink] can
 * validate the link when it comes back.
 *
 * [credentialForLinking] only adds the provider id to that URL — it is **not** persisted here. A
 * caller linking a collided social credential must save it itself, via
 * `EmailLinkPersistenceManager.saveCredentialForLinking`, before calling this; that is what
 * [signInWithEmailLink] later picks up.
 */
internal suspend fun AuthFlowScope.sendSignInLinkToEmail(
    context: Context,
    provider: AuthProvider.Email,
    email: String,
    credentialForLinking: AuthCredential?,
    persistenceManager: PersistenceManager = EmailLinkPersistenceManager.default,
) {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSendingEmailLink))

        // Get anonymousUserId if can upgrade anonymously else default to empty string.
        // NOTE: check for empty string instead of null to validate anonymous user ID matches
        // when sign in from email link
        val anonymousUserId =
            if (canUpgradeAnonymous(config, auth)) (auth.currentUser?.uid
                ?: "") else ""

        // Generate sessionId
        val sessionId =
            SessionUtils.generateRandomAlphaNumericString(AuthProvider.Email.SESSION_ID_LENGTH)

        // Modify actionCodeSettings Url to include sessionId, anonymousUserId, force same
        // device flag
        val updatedActionCodeSettings =
            provider.addSessionInfoToActionCodeSettings(
                sessionId = sessionId,
                anonymousUserId = anonymousUserId,
                credentialForLinking = credentialForLinking
            )

        auth.sendSignInLinkToEmail(email, updatedActionCodeSettings).await()

        // Save Email to dataStore for use in signInWithEmailLink
        persistenceManager.saveEmail(context, email, sessionId, anonymousUserId)

        emit(AuthState.EmailSignInLinkSent())
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Send sign in link to email was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Completes a passwordless sign-in from the link the user followed.
 *
 * On the same device the address and session id come from storage and the user is signed in
 * without further input. When the session id does not match — a different device, or storage
 * cleared — an empty [email] raises [AuthException.EmailLinkPromptForEmailException]; call again
 * with the address the user supplies. On the same-device path an empty [email] instead means the
 * stored address is gone, and raises [AuthException.EmailMismatchException].
 *
 * @throws AuthException.EmailLinkWrongDeviceException if the link requires the originating device
 * — force-same-device, or an anonymous upgrade — and was opened elsewhere.
 * @throws AuthException.EmailLinkCrossDeviceLinkingException if a link carrying a social
 * credential to link is opened on another device.
 * @throws AuthException.EmailLinkDifferentAnonymousUserException if the anonymous uid in the link
 * is not the uid signed in now.
 * @throws AuthException.InvalidEmailLinkException if the link is not a sign-in link.
 */
internal suspend fun AuthFlowScope.signInWithEmailLink(
    context: Context,
    provider: AuthProvider.Email,
    email: String,
    emailLink: String,
    persistenceManager: PersistenceManager = EmailLinkPersistenceManager.default,
): AuthResult? {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSigningInWithEmailLink))

        // Validate link format
        if (!auth.isSignInWithEmailLink(emailLink)) {
            throw AuthException.InvalidEmailLinkException()
        }

        // Parse email link for session data
        val parser = EmailLinkParser(emailLink)
        val sessionIdFromLink = parser.sessionId
        val anonymousUserIdFromLink = parser.anonymousUserId
        val oobCode = parser.oobCode
        val providerIdFromLink = parser.providerId
        val isEmailLinkForceSameDeviceEnabled = parser.forceSameDeviceBit

        // Retrieve stored session record from DataStore
        val sessionRecord = persistenceManager.retrieveSessionRecord(context)
        val storedSessionId = sessionRecord?.sessionId

        // Check if this is a different device flow
        val isDifferentDevice = provider.isDifferentDevice(
            sessionIdFromLocal = storedSessionId,
            sessionIdFromLink = sessionIdFromLink ?: "" // Convert null to empty string to match legacy behavior
        )

        if (isDifferentDevice) {
            // Handle cross-device flow
            // Session ID must always be present in the link
            if (sessionIdFromLink.isNullOrEmpty()) {
                val exception = AuthException.InvalidEmailLinkException()
                emit(AuthState.Error(exception))
                throw exception
            }

            // These scenarios require same-device flow
            if (isEmailLinkForceSameDeviceEnabled || !anonymousUserIdFromLink.isNullOrEmpty()) {
                val exception = AuthException.EmailLinkWrongDeviceException()
                emit(AuthState.Error(exception))
                throw exception
            }

            // If we have no SessionRecord/there is a session ID mismatch, this means that we were
            // not the ones to send the link. The only way forward is to prompt the user for their
            // email before continuing the flow. We should only do that after validating the link.
            // However, if email is already provided (cross-device with user input), skip validation
            if (email.isEmpty()) {
                handleDifferentDeviceErrorFlow(oobCode, providerIdFromLink, emailLink)
                return null
            }
            // Email provided - validate it and continue with normal flow
        }

        // Validate email is not empty (same-device flow only)
        if (email.isEmpty()) {
            throw AuthException.EmailMismatchException()
        }

        // Validate anonymous user ID matches (same-device flow)
        if (!anonymousUserIdFromLink.isNullOrEmpty()) {
            val currentUser = auth.currentUser
            if (currentUser == null
                || !currentUser.isAnonymous
                || currentUser.uid != anonymousUserIdFromLink
            ) {
                val exception = AuthException.EmailLinkDifferentAnonymousUserException()
                emit(AuthState.Error(exception))
                throw exception
            }
        }

        // Get credential for linking from session record
        val storedCredentialForLink = sessionRecord?.credentialForLinking
        val emailLinkCredential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        val result = if (storedCredentialForLink == null) {
            // Normal Flow: Just sign in with email link
            handleEmailLinkNormalFlow(emailLinkCredential)
        } else {
            // Linking Flow: Sign in with email link, then link the social credential
            handleEmailLinkCredentialLinkingFlow(
                context = context,
                email = email,
                emailLinkCredential = emailLinkCredential,
                storedCredentialForLink = storedCredentialForLink,
            )
        }
        // Clear DataStore after success
        persistenceManager.clear(context)
        // In reauth mode the stamped Success is already published and there is no AuthResult, so
        // emitResult would overwrite the stamp with Idle and orphan the operation.
        if (result == null && config.isReauthenticationMode) {
            return null
        }
        emitResult(result)
        return result
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with email link was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}

private suspend fun AuthFlowScope.handleDifferentDeviceErrorFlow(
    oobCode: String,
    providerIdFromLink: String?,
    emailLink: String
) {
    // Validate the action code
    try {
        auth.checkActionCode(oobCode).await()
    } catch (e: Exception) {
        // Invalid action code
        val exception = AuthException.InvalidEmailLinkException(cause = e)
        emit(AuthState.Error(exception))
        throw exception
    }

    // If there's a provider ID, this is a linking flow which can't be done cross-device
    if (!providerIdFromLink.isNullOrEmpty()) {
        val providerNameForMessage =
            Provider.fromId(providerIdFromLink)?.providerName ?: providerIdFromLink
        val exception = AuthException.EmailLinkCrossDeviceLinkingException(
            providerName = providerNameForMessage,
            emailLink = emailLink
        )
        emit(AuthState.Error(exception))
        throw exception
    }

    // Link is valid but we need the user to provide their email
    val exception = AuthException.EmailLinkPromptForEmailException(
        cause = null,
        emailLink = emailLink
    )
    emit(AuthState.Error(exception))
    throw exception
}

private suspend fun AuthFlowScope.handleEmailLinkNormalFlow(
    emailLinkCredential: AuthCredential,
): AuthResult? {
    return signInAndLinkWithCredential(emailLinkCredential)
}

private suspend fun AuthFlowScope.handleEmailLinkCredentialLinkingFlow(
    context: Context,
    email: String,
    emailLinkCredential: AuthCredential,
    storedCredentialForLink: AuthCredential,
): AuthResult? {
    return if (canUpgradeAnonymous(config, auth)) {
        // Anonymous upgrade: Use safe link pattern with scratch auth
        val appExplicitlyForValidation = FirebaseApp.initializeApp(
            context,
            auth.app.options,
            "FUIAuthScratchApp_${System.currentTimeMillis()}"
        )
        val authExplicitlyForValidation = FirebaseAuth
            .getInstance(appExplicitlyForValidation)

        // Safe link: Validate that both credentials can be linked
        authExplicitlyForValidation
            .signInWithCredential(emailLinkCredential).await()
            .user?.linkWithCredential(storedCredentialForLink)?.await()
            .also { result ->
                // If safe link succeeds, throw AccountLinkingRequiredException for UI to handle
                val accountLinkingException = AuthException.AccountLinkingRequiredException(
                    message = "An account already exists with this email. " +
                            "Please sign in with your existing account to upgrade your anonymous account.",
                    email = email,
                    credential = storedCredentialForLink,
                    cause = null
                )
                emit(AuthState.Error(accountLinkingException))
                throw accountLinkingException
            }
    } else {
        // Non-upgrade: Sign in with email link, then link social credential
        auth.signInWithCredential(emailLinkCredential).await()
            // Link the social credential
            .user?.linkWithCredential(storedCredentialForLink)?.await()
            .also { result ->
                result?.user?.let { user ->
                    // Merge profile from the linked social credential
                    mergeProfile(
                        auth,
                        user.displayName,
                        user.photoUrl
                    )
                }
            }
    }
}

/**
 * Sends a password reset email to [email] and emits [AuthState.PasswordResetLinkSent].
 *
 * [actionCodeSettings] points the link at your own page instead of the Firebase-hosted one.
 */
internal suspend fun AuthFlowScope.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings? = null,
) {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSendingPasswordResetEmail))
        auth.sendPasswordResetEmail(email, actionCodeSettings).await()
        emit(AuthState.PasswordResetLinkSent())
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Send password reset email was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}
