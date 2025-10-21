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
import android.net.Uri
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider.Companion.canUpgradeAnonymous
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider.Companion.mergeProfile
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.data.EmailLinkParser
import com.firebase.ui.auth.util.data.SessionUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await


/**
 * Creates an email/password account or links the credential to an anonymous user.
 *
 * Mirrors the legacy email sign-up handler: validates password strength, validates custom
 * password rules, checks if new accounts are allowed, chooses between
 * `createUserWithEmailAndPassword` and `linkWithCredential`, merges the supplied display name
 * into the Firebase profile, and throws [AuthException.AccountLinkingRequiredException] when
 * anonymous upgrade encounters an existing account for the email.
 *
 * **Flow:**
 * 1. Check if new accounts are allowed (for non-upgrade flows)
 * 2. Validate password length against [AuthProvider.Email.minimumPasswordLength]
 * 3. Validate password against custom [AuthProvider.Email.passwordValidationRules]
 * 4. If upgrading anonymous user: link credential to existing anonymous account
 * 5. Otherwise: create new account with `createUserWithEmailAndPassword`
 * 6. Merge display name into user profile
 *
 * @param context Android [Context] for localized strings
 * @param config Auth UI configuration describing provider settings
 * @param provider Email provider configuration
 * @param name Optional display name collected during sign-up
 * @param email Email address for the new account
 * @param password Password for the new account
 *
 * @return [AuthResult] containing the newly created or linked user, or null if failed
 *
 * @throws AuthException.UserNotFoundException if new accounts are not allowed
 * @throws AuthException.WeakPasswordException if the password fails validation rules
 * @throws AuthException.InvalidCredentialsException if the email or password is invalid
 * @throws AuthException.EmailAlreadyInUseException if the email already exists
 * @throws AuthException.AuthCancelledException if the coroutine is cancelled
 * @throws AuthException.NetworkException for network-related failures
 *
 * **Example: Normal sign-up**
 * ```kotlin
 * try {
 *     val result = firebaseAuthUI.createOrLinkUserWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         name = "John Doe",
 *         email = "john@example.com",
 *         password = "SecurePass123!"
 *     )
 *     // User account created successfully
 * } catch (e: AuthException.WeakPasswordException) {
 *     // Password doesn't meet validation rules
 * } catch (e: AuthException.EmailAlreadyInUseException) {
 *     // Email already exists - redirect to sign-in
 * }
 * ```
 *
 * **Example: Anonymous user upgrade**
 * ```kotlin
 * // User is currently signed in anonymously
 * try {
 *     val result = firebaseAuthUI.createOrLinkUserWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         name = "Jane Smith",
 *         email = "jane@example.com",
 *         password = "MyPassword456"
 *     )
 *     // Anonymous account upgraded to permanent email/password account
 * } catch (e: AuthException.AccountLinkingRequiredException) {
 *     // Email already exists - show account linking UI
 *     // User needs to sign in with existing account to link
 * }
 * ```
 */
// TODO(demolaf): make this internal
suspend fun FirebaseAuthUI.createOrLinkUserWithEmailAndPassword(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    name: String?,
    email: String,
    password: String,
    credentialProvider: AuthProvider.Email.CredentialProvider = AuthProvider.Email.DefaultCredentialProvider(),
): AuthResult? {
    val canUpgrade = canUpgradeAnonymous(config, auth)
    val pendingCredential =
        if (canUpgrade) credentialProvider.getCredential(email, password) else null

    try {
        // Check if new accounts are allowed (only for non-upgrade flows)
        if (!canUpgrade && !provider.isNewAccountsAllowed) {
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

        updateAuthState(AuthState.Loading("Creating user..."))
        val result = if (canUpgrade) {
            auth.currentUser?.linkWithCredential(requireNotNull(pendingCredential))?.await()
        } else {
            auth.createUserWithEmailAndPassword(email, password).await()
        }.also { authResult ->
            authResult?.user?.let {
                // Merge display name into profile (photoUri is always null for email/password)
                mergeProfile(auth, name, null)
            }
        }
        updateAuthState(AuthState.Idle)
        return result
    } catch (e: FirebaseAuthUserCollisionException) {
        // Account collision: email already exists
        val accountLinkingException = AuthException.AccountLinkingRequiredException(
            message = "An account already exists with this email. " +
                    "Please sign in with your existing account.",
            email = e.email,
            credential = if (canUpgrade) {
                e.updatedCredential ?: pendingCredential
            } else {
                null
            },
            cause = e
        )
        updateAuthState(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Create or link user with email and password was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Signs in a user with email and password, optionally linking a social credential.
 *
 * This method handles both normal sign-in and anonymous upgrade flows. In anonymous upgrade
 * scenarios, it validates credentials in a scratch auth instance before throwing
 * [AuthException.AccountLinkingRequiredException].
 *
 * **Flow:**
 * 1. If anonymous upgrade:
 *    - Create scratch auth instance to validate credential
 *    - If linking social provider: sign in with email, then link social credential (safe link)
 *    - Otherwise: just validate email credential
 *    - Throw [AuthException.AccountLinkingRequiredException] after successful validation
 * 2. If normal sign-in:
 *    - Sign in with email/password
 *    - If credential provided: link it and merge profile
 *
 * @param context Android [Context] for creating scratch auth instance
 * @param config Auth UI configuration describing provider settings
 * @param email Email address for sign-in
 * @param password Password for sign-in
 * @param credentialForLinking Optional social provider credential to link after sign-in
 *
 * @return [AuthResult] containing the signed-in user, or null if validation-only (anonymous upgrade)
 *
 * @throws AuthException.InvalidCredentialsException if email or password is incorrect
 * @throws AuthException.UserNotFoundException if the user doesn't exist
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException for network-related failures
 *
 * **Example: Normal sign-in**
 * ```kotlin
 * try {
 *     val result = firebaseAuthUI.signInWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         email = "user@example.com",
 *         password = "password123"
 *     )
 *     // User signed in successfully
 * } catch (e: AuthException.InvalidCredentialsException) {
 *     // Wrong password
 * }
 * ```
 *
 * **Example: Sign-in with social credential linking**
 * ```kotlin
 * // User tried to sign in with Google, but account exists with email/password
 * // Prompt for password, then link Google credential
 * val googleCredential = GoogleAuthProvider.getCredential(idToken, null)
 *
 * val result = firebaseAuthUI.signInWithEmailAndPassword(
 *     context = context,
 *     config = authUIConfig,
 *     provider = emailProvider,
 *     email = "user@example.com",
 *     password = "password123",
 *     credentialForLinking = googleCredential
 * )
 * // User signed in with email/password AND Google is now linked
 * // Profile updated with Google display name and photo
 * ```
 *
 * **Example: Anonymous upgrade validation**
 * ```kotlin
 * // User is anonymous, wants to upgrade with existing email/password account
 * try {
 *     firebaseAuthUI.signInWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         email = "existing@example.com",
 *         password = "password123"
 *     )
 * } catch (e: AuthException.AccountLinkingRequiredException) {
 *     // Account linking required - UI shows account linking screen
 *     // User needs to sign in with existing account to link anonymous account
 * }
 * ```
 */
internal suspend fun FirebaseAuthUI.signInWithEmailAndPassword(
    context: Context,
    config: AuthUIConfiguration,
    email: String,
    password: String,
    credentialForLinking: AuthCredential? = null,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in..."))
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
                        updateAuthState(AuthState.Error(accountLinkingException))
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
                        updateAuthState(AuthState.Error(accountLinkingException))
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
        }.also {
            updateAuthState(AuthState.Idle)
        }
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with email and password was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Signs in with a credential or links it to an existing anonymous user.
 *
 * This method handles both normal sign-in and anonymous upgrade flows. After successful
 * authentication, it merges profile information (display name and photo URL) into the
 * Firebase user profile if provided.
 *
 * **Flow:**
 * 1. Check if user is anonymous and upgrade is enabled
 * 2. If yes: Link credential to anonymous user
 * 3. If no: Sign in with credential
 * 4. Merge profile information (name, photo) into Firebase user
 * 5. Handle collision exceptions by throwing [AuthException.AccountLinkingRequiredException]
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param credential The [AuthCredential] to use for authentication. Can be from any provider.
 * @param displayName Optional display name from the provider to merge into the user profile
 * @param photoUrl Optional photo URL from the provider to merge into the user profile
 *
 * @return [AuthResult] containing the authenticated user
 *
 * @throws AuthException.InvalidCredentialsException if credential is invalid or expired
 * @throws AuthException.EmailAlreadyInUseException if linking and email is already in use
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 *
 * **Example: Google Sign-In**
 * ```kotlin
 * val googleCredential = GoogleAuthProvider.getCredential(idToken, null)
 * val displayName = "John Doe"  // From Google profile
 * val photoUrl = Uri.parse("https://...")  // From Google profile
 *
 * val result = firebaseAuthUI.signInAndLinkWithCredential(
 *     config = authUIConfig,
 *     credential = googleCredential,
 *     displayName = displayName,
 *     photoUrl = photoUrl
 * )
 * // User signed in with Google AND profile updated with Google data
 * ```
 *
 * **Example: Phone Auth**
 * ```kotlin
 * val phoneCredential = PhoneAuthProvider.getCredential(verificationId, code)
 *
 * val result = firebaseAuthUI.signInAndLinkWithCredential(
 *     config = authUIConfig,
 *     credential = phoneCredential
 * )
 * // User signed in with phone number
 * ```
 *
 * **Example: Phone Auth with Collision (Anonymous Upgrade)**
 * ```kotlin
 * // User is currently anonymous, trying to link a phone number
 * val phoneCredential = PhoneAuthProvider.getCredential(verificationId, code)
 *
 * try {
 *     firebaseAuthUI.signInAndLinkWithCredential(
 *         config = authUIConfig,
 *         credential = phoneCredential
 *     )
 * } catch (e: AuthException.AccountLinkingRequiredException) {
 *     // Phone number already exists on another account
 *     // Account linking required - UI can show account linking screen
 *     // User needs to sign in with existing account to link
 * }
 * ```
 *
 * **Example: Email Link Sign-In**
 * ```kotlin
 * val emailLinkCredential = EmailAuthProvider.getCredentialWithLink(
 *     email = "user@example.com",
 *     emailLink = emailLink
 * )
 *
 * val result = firebaseAuthUI.signInAndLinkWithCredential(
 *     config = authUIConfig,
 *     credential = emailLinkCredential
 * )
 * // User signed in with email link (passwordless)
 * ```
 */
internal suspend fun FirebaseAuthUI.signInAndLinkWithCredential(
    config: AuthUIConfiguration,
    credential: AuthCredential,
    provider: AuthProvider? = null,
    displayName: String? = null,
    photoUrl: Uri? = null,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in user..."))
        return if (canUpgradeAnonymous(config, auth)) {
            auth.currentUser?.linkWithCredential(credential)?.await()
        } else {
            auth.signInWithCredential(credential).await()
        }.also { result ->
            // Merge profile information from the provider
            result?.user?.let {
                mergeProfile(auth, displayName, photoUrl)
            }
            updateAuthState(AuthState.Idle)
        }
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
                    "your ${provider?.name ?: "this provider"} account.",
            email = email,
            credential = credentialForException,
            cause = e
        )
        updateAuthState(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in and link with credential was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Sends a passwordless sign-in link to the specified email address.
 *
 * This method initiates the email-link (passwordless) authentication flow by sending
 * an email containing a magic link. The link includes session information for validation
 * and security.
 *
 * **How it works:**
 * 1. Generates a unique session ID for same-device validation
 * 2. Retrieves anonymous user ID if upgrading anonymous account
 * 3. Enriches the [ActionCodeSettings] URL with session data (session ID, anonymous user ID, force same-device flag)
 * 4. Sends the email via [com.google.firebase.auth.FirebaseAuth.sendSignInLinkToEmail]
 * 5. Saves session data to DataStore for validation when the user clicks the link
 * 6. User receives email with a magic link containing the session information
 * 7. When user clicks link, app opens via deep link and calls [signInWithEmailLink] to complete authentication
 *
 * **Account Linking Support:**
 * If a user tries to sign in with a social provider (Google, Facebook) but an email link
 * account already exists with that email, the social provider implementation should:
 * 1. Catch the [FirebaseAuthUserCollisionException] from the sign-in attempt
 * 2. Call [EmailLinkPersistenceManager.saveCredentialForLinking] with the provider tokens
 * 3. Call this method to send the email link
 * 4. When [signInWithEmailLink] completes, it automatically retrieves and links the saved credential
 *
 * **Session Security:**
 * - **Session ID**: Random 10-character string for same-device validation
 * - **Anonymous User ID**: Stored if upgrading anonymous account to prevent account hijacking
 * - **Force Same Device**: Can be configured via [AuthProvider.Email.isEmailLinkForceSameDeviceEnabled]
 * - All session data is validated in [signInWithEmailLink] before completing authentication
 *
 * @param context Android [Context] for DataStore access
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param provider The [AuthProvider.Email] configuration with [ActionCodeSettings]
 * @param email The email address to send the sign-in link to
 *
 * @throws AuthException.InvalidCredentialsException if email is invalid
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 * @throws IllegalStateException if ActionCodeSettings is not configured
 *
 * **Example 1: Basic email link sign-in**
 * ```kotlin
 * // Send the email link
 * firebaseAuthUI.sendSignInLinkToEmail(
 *     context = context,
 *     config = authUIConfig,
 *     provider = emailProvider,
 *     email = "user@example.com"
 * )
 * // Show "Check your email" UI to user
 *
 * // Later, when user clicks the link in their email:
 * // (In your deep link handling Activity)
 * val emailLink = intent.data.toString()
 * firebaseAuthUI.signInWithEmailLink(
 *     context = context,
 *     config = authUIConfig,
 *     provider = emailProvider,
 *     email = "user@example.com",
 *     emailLink = emailLink
 * )
 * // User is now signed in
 * ```
 *
 * **Example 2: Anonymous user upgrade**
 * ```kotlin
 * // User is currently signed in anonymously
 * // Send email link to upgrade anonymous account to permanent email account
 * firebaseAuthUI.sendSignInLinkToEmail(
 *     context = context,
 *     config = authUIConfig,
 *     provider = emailProvider,
 *     email = "user@example.com"
 * )
 * // Session includes anonymous user ID for validation
 * // When user clicks link, anonymous account is upgraded to permanent account
 * ```
 * @see signInWithEmailLink
 * @see EmailLinkPersistenceManager.saveCredentialForLinking
 * @see com.google.firebase.auth.FirebaseAuth.sendSignInLinkToEmail
 */
internal suspend fun FirebaseAuthUI.sendSignInLinkToEmail(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    email: String,
) {
    try {
        updateAuthState(AuthState.Loading("Sending sign in email link..."))

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
            provider.addSessionInfoToActionCodeSettings(sessionId, anonymousUserId)

        auth.sendSignInLinkToEmail(email, updatedActionCodeSettings).await()

        // Save Email to dataStore for use in signInWithEmailLink
        EmailLinkPersistenceManager.saveEmail(context, email, sessionId, anonymousUserId)

        updateAuthState(AuthState.EmailSignInLinkSent())
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Send sign in link to email was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Signs in a user using an email link (passwordless authentication).
 *
 * This method completes the email link sign-in flow after the user clicks the magic link
 * sent to their email. It validates the link, extracts session information, and either
 * signs in the user normally or upgrades an anonymous account based on configuration.
 *
 * **Flow:**
 * 1. User receives email with magic link
 * 2. User clicks link, app opens via deep link
 * 3. Activity extracts emailLink from Intent.data
 * 4. This method validates and completes sign-in
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param provider The [AuthProvider.Email] configuration with email-link settings
 * @param email The email address of the user (retrieved from DataStore or user input)
 * @param emailLink The complete deep link URL received from the Intent.
 *
 * This URL contains:
 * - Firebase action code (oobCode) for authentication
 * - Session ID (ui_sid) for same-device validation
 * - Anonymous user ID (ui_auid) if upgrading anonymous account
 * - Force same-device flag (ui_sd) for security enforcement
 *
 * Example:
 * `https://yourapp.page.link/emailSignIn?oobCode=ABC123&continueUrl=...`
 *
 * @throws AuthException.InvalidCredentialsException if the email link is invalid or expired
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 * @throws AuthException.UnknownException for other errors
 *
 * @see sendSignInLinkToEmail for sending the initial email link
 */
internal suspend fun FirebaseAuthUI.signInWithEmailLink(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    email: String,
    emailLink: String,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in with email link..."))

        // Validate link format
        if (!auth.isSignInWithEmailLink(emailLink)) {
            throw AuthException.InvalidEmailLinkException()
        }

        // Validate email is not empty
        if (email.isEmpty()) {
            throw AuthException.EmailMismatchException()
        }

        // Parse email link for session data
        val parser = EmailLinkParser(emailLink)
        val sessionIdFromLink = parser.sessionId
        val anonymousUserIdFromLink = parser.anonymousUserId
        val oobCode = parser.oobCode
        val providerIdFromLink = parser.providerId
        val isEmailLinkForceSameDeviceEnabled = parser.forceSameDeviceBit

        // Retrieve stored session record from DataStore
        val sessionRecord = EmailLinkPersistenceManager.retrieveSessionRecord(context)
        val storedSessionId = sessionRecord?.sessionId

        // Check if this is a different device flow
        val isDifferentDevice = provider.isDifferentDevice(
            sessionIdFromLocal = storedSessionId,
            sessionIdFromLink = sessionIdFromLink
        )

        if (isDifferentDevice) {
            // Handle cross-device flow
            // Session ID must always be present in the link
            if (sessionIdFromLink.isNullOrEmpty()) {
                throw AuthException.InvalidEmailLinkException()
            }

            // These scenarios require same-device flow
            if (isEmailLinkForceSameDeviceEnabled || !anonymousUserIdFromLink.isNullOrEmpty()) {
                throw AuthException.EmailLinkWrongDeviceException()
            }

            // Validate the action code
            auth.checkActionCode(oobCode).await()

            // If there's a provider ID, this is a linking flow which can't be done cross-device
            if (!providerIdFromLink.isNullOrEmpty()) {
                throw AuthException.EmailLinkCrossDeviceLinkingException()
            }

            // Link is valid but we need the user to provide their email
            throw AuthException.EmailLinkPromptForEmailException()
        }

        // Validate anonymous user ID matches (same-device flow)
        if (!anonymousUserIdFromLink.isNullOrEmpty()) {
            val currentUser = auth.currentUser
            if (currentUser == null
                || !currentUser.isAnonymous
                || currentUser.uid != anonymousUserIdFromLink
            ) {
                throw AuthException.EmailLinkDifferentAnonymousUserException()
            }
        }

        // Get credential for linking from session record
        val storedCredentialForLink = sessionRecord?.credentialForLinking
        val emailLinkCredential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        val result = if (storedCredentialForLink == null) {
            // Normal Flow: Just sign in with email link
            signInAndLinkWithCredential(config, emailLinkCredential)
        } else {
            // Linking Flow: Sign in with email link, then link the social credential
            if (canUpgradeAnonymous(config, auth)) {
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
                        updateAuthState(AuthState.Error(accountLinkingException))
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
        // Clear DataStore after success
        EmailLinkPersistenceManager.clear(context)
        updateAuthState(AuthState.Idle)
        return result
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with email link was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

/**
 * Sends a password reset email to the specified email address.
 *
 * This method initiates the "forgot password" flow by sending an email to the user
 * with a link to reset their password. The user will receive an email from Firebase
 * containing a link that allows them to set a new password for their account.
 *
 * **Flow:**
 * 1. Validate the email address exists in Firebase Auth
 * 2. Send password reset email to the user
 * 3. Emit [AuthState.PasswordResetLinkSent] state
 * 4. User clicks link in email to reset password
 * 5. User is redirected to Firebase-hosted password reset page (or custom URL if configured)
 *
 * **Error Handling:**
 * - If the email doesn't exist: throws [AuthException.UserNotFoundException]
 * - If the email is invalid: throws [AuthException.InvalidCredentialsException]
 * - If network error occurs: throws [AuthException.NetworkException]
 *
 * @param email The email address to send the password reset email to
 * @param actionCodeSettings Optional [ActionCodeSettings] to configure the password reset link.
 *                           Use this to customize the continue URL, dynamic link domain, and other settings.
 *
 * @throws AuthException.UserNotFoundException if no account exists with this email
 * @throws AuthException.InvalidCredentialsException if the email format is invalid
 * @throws AuthException.NetworkException if a network error occurs
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.UnknownException for other errors
 *
 * **Example 1: Basic password reset**
 * ```kotlin
 * try {
 *     firebaseAuthUI.sendPasswordResetEmail(
 *         email = "user@example.com"
 *     )
 *     // Show success message: "Password reset email sent to $email"
 * } catch (e: AuthException.UserNotFoundException) {
 *     // Show error: "No account exists with this email"
 * } catch (e: AuthException.InvalidCredentialsException) {
 *     // Show error: "Invalid email address"
 * }
 * ```
 *
 * **Example 2: Custom password reset with ActionCodeSettings**
 * ```kotlin
 * val actionCodeSettings = ActionCodeSettings.newBuilder()
 *     .setUrl("https://myapp.com/resetPassword")  // Continue URL after reset
 *     .setHandleCodeInApp(false)  // Use Firebase-hosted reset page
 *     .setAndroidPackageName(
 *         "com.myapp",
 *         true,  // Install if not available
 *         null   // Minimum version
 *     )
 *     .build()
 *
 * firebaseAuthUI.sendPasswordResetEmail(
 *     email = "user@example.com",
 *     actionCodeSettings = actionCodeSettings
 * )
 * // User receives email with custom continue URL
 * ```
 *
 * @see com.google.firebase.auth.ActionCodeSettings
 */
internal suspend fun FirebaseAuthUI.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings? = null,
) {
    try {
        updateAuthState(AuthState.Loading("Sending password reset email..."))
        auth.sendPasswordResetEmail(email, actionCodeSettings).await()
        updateAuthState(AuthState.PasswordResetLinkSent())
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Send password reset email was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}
