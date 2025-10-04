package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.util.data.EmailLinkParser
import com.firebase.ui.auth.util.data.SessionUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Creates a new user with email and password, or links to an existing anonymous user.
 *
 * This method handles both new user creation and anonymous user upgrade scenarios.
 * After successful account creation, it automatically updates the user's profile with
 * the provided display name and photo URI.
 *
 * **Flow:**
 * 1. Check if user is anonymous and upgrade is enabled
 * 2. If yes: Link email/password credential to anonymous user
 * 3. If no: Create new user with email/password
 * 4. Update profile with display name and photo (if provided)
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param provider
 * @param newUser Optional User to merge existing user profile
 * @param email The email address for the new account
 * @param password The password for the new account
 *
 * @throws AuthException.WeakPasswordException if password doesn't meet requirements
 * @throws AuthException.InvalidCredentialsException if email is invalid
 * @throws AuthException.EmailAlreadyInUseException if email is already registered
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 *
 * **Old library reference:**
 * - EmailProviderResponseHandler.java:55-58 (createOrLinkUserWithEmailAndPassword call)
 * - EmailProviderResponseHandler.java:59 (ProfileMerger continuation)
 * - AuthOperationManager.java:64-74 (implementation)
 * - RegisterEmailFragment.java:279-285 (UI calling this method)
 */
internal suspend fun FirebaseAuthUI.createOrLinkUserWithEmailAndPassword(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    newUser: User? = null,
    email: String,
    password: String
) {
    try {
        if (password.length < provider.minimumPasswordLength) {
            throw AuthException.InvalidCredentialsException(
                message = context.getString(R.string.fui_error_password_too_short)
                    .format(provider.minimumPasswordLength)
            )
        }
        updateAuthState(AuthState.Loading("Creating user..."))
        if (AuthProvider.canUpgradeAnonymous(config, auth)) {
            val credential = EmailAuthProvider.getCredential(email, password)
            auth.currentUser?.linkWithCredential(credential)?.await()
        } else {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
        AuthProvider.mergeProfile(auth, newUser?.name, newUser?.photoUri)
        updateAuthState(AuthState.Idle)
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
 * Signs in a user with email and password, optionally linking a social provider credential.
 *
 * This method handles normal sign-in and anonymous user upgrade scenarios. If a social
 * provider credential (e.g., Google, Facebook) is provided, it will be linked to the
 * account after successful sign-in.
 *
 * **For anonymous upgrade scenarios:** The UI layer should first validate credentials
 * and show a merge conflict dialog before calling this method.
 *
 * **Flow:**
 * 1. Check if user is anonymous and upgrade is enabled
 * 2. If yes: Link email/password credential to anonymous user
 * 3. If no: Sign in with email/password
 * 4. If credentialForLinking is provided: Link it to the account
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param provider
 * @param existingUser Optional User to merge existing user profile
 * @param email The email address of the user
 * @param password The password for the account
 * @param credentialForLinking Optional [AuthCredential] from a social provider (Google,
 * Facebook) that should be linked to the account after sign-in. This is used when a user
 * tries to sign in with a social provider but an email/password account with the same email
 * already exists.
 *
 * @throws AuthException.InvalidCredentialsException if email or password is incorrect
 * @throws AuthException.UserNotFoundException if no account exists with this email
 * @throws AuthException.TooManyRequestsException if too many sign-in attempts
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 *
 * **Old library reference:**
 * - WelcomeBackPasswordHandler.java:96-117 (normal sign-in with credential linking)
 * - WelcomeBackPasswordHandler.java:106-108 (linkWithCredential call)
 * - WelcomeBackPasswordPrompt.java:183 (UI calling this method)
 */
internal suspend fun FirebaseAuthUI.signInWithEmailAndPassword(
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    existingUser: User? = null,
    email: String,
    password: String,
    credentialForLinking: AuthCredential? = null,
) {
    try {
        updateAuthState(AuthState.Loading("Signing in..."))

        if (AuthProvider.canUpgradeAnonymous(config, auth)) {
            // Link email/password credential to anonymous user
            val credentialToValidate = EmailAuthProvider
                .getCredential(email, password)

            val isSocialProvider = provider.providerId in listOf(
                Provider.GOOGLE.id,
                Provider.FACEBOOK.id,
            )

            // Like scratch auth, this is used to avoid losing the anonymous user state in
            // the main auth instance
            val clonedAuth = FirebaseAuth
                .getInstance(FirebaseApp.getInstance(app.name))

            // Safe Link
            // Add the provider to the same account before triggering a merge failure.
            val credentialValidationResult = clonedAuth
                .signInWithCredential(credentialToValidate).await()

            // Check to see if we need to link (for social providers with the same email)
            if (isSocialProvider && credentialForLinking != null) {
                val linkResult = credentialValidationResult
                    .user?.linkWithCredential(credentialForLinking)?.await()

                if (linkResult?.user != null) {
                    // Update AuthState with a firebase auth merge failure
                    return updateAuthState(AuthState.MergeConflict(credentialToValidate))
                }
            } else {
                // The user has not tried to log in with a federated IDP containing the same email.
                // In this case, we just need to verify that the credential they provided is valid.
                // No linking is done for non-federated IDPs.
                // A merge failure occurs because the account exists and the user is anonymous.
                if (credentialValidationResult?.user != null) {
                    // Update AuthState with a firebase auth merge failure
                    return updateAuthState(AuthState.MergeConflict(credentialToValidate))
                }
            }
        } else {
            // Normal sign-in
            val result = auth.signInWithEmailAndPassword(email, password).await()

            // If there's a credential to link (e.g., from Google/Facebook),
            // link it to the account after sign-in
            if (credentialForLinking != null) {
                result.user?.linkWithCredential(credentialForLinking)?.await()

                // Note: Profile info from social provider (displayName, photoUri) should be
                // extracted by the UI layer and passed to provider.mergeProfile()
                // For now, this is left to the UI implementation
                AuthProvider.mergeProfile(auth, existingUser?.name, existingUser?.photoUri)
            }
        }

        updateAuthState(AuthState.Idle)
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
 * **Flow:**
 * 1. Check if user is anonymous and upgrade is enabled
 * 2. If yes: Link credential to anonymous user
 * 3. If no: Sign in with credential
 *
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param credential The [AuthCredential] to use for authentication. Can be from any provider.
 *
 * @throws AuthException.InvalidCredentialsException if credential is invalid or expired
 * @throws AuthException.EmailAlreadyInUseException if linking and email is already in use
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 *
 * **Old library reference:**
 * - AuthOperationManager.java:76-84 (signInAndLinkWithCredential implementation)
 * - EmailLinkSignInHandler.java:217 (calling this with email-link credential)
 * - SocialProviderResponseHandler
 * - PhoneProviderResponseHandler
 */
internal suspend fun FirebaseAuthUI.signInAndLinkWithCredential(
    config: AuthUIConfiguration,
    credential: AuthCredential
) {
    try {
        updateAuthState(AuthState.Loading("Signing in user..."))
        if (AuthProvider.canUpgradeAnonymous(config, auth)) {
            auth.currentUser?.linkWithCredential(credential)?.await()
        } else {
            auth.signInWithCredential(credential).await()
        }
        updateAuthState(AuthState.Idle)
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
 * **Flow:**
 * 1. Generate unique session ID
 * 2. Get anonymous user ID if upgrading
 * 3. Enrich ActionCodeSettings with session data
 * 4. Send email via Firebase Auth
 * 5. Save session data to DataStore for later validation
 *
 * **After this method:**
 * - User receives email with magic link
 * - User clicks link → app opens via deep link
 * - App calls [signInWithEmailLink] to complete sign-in
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
 * **Old library reference:**
 * - EmailLinkSendEmailHandler.java:26-55 (complete implementation)
 * - EmailLinkSendEmailHandler.java:38-39 (session ID generation)
 * - EmailLinkSendEmailHandler.java:47-48 (DataStore persistence)
 *
 * @see com.google.firebase.auth.FirebaseAuth.signInWithEmailLink
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
            if (AuthProvider.canUpgradeAnonymous(config, auth)) (auth.currentUser?.uid
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

        updateAuthState(AuthState.Idle)
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
 * **Old library reference:**
 * - EmailLinkSignInHandler.java:43-100 (complete validation and sign-in flow)
 * - EmailLinkSignInHandler.java:53-56 (retrieve session from DataStore)
 * - EmailLinkSignInHandler.java:58-63 (parse link using EmailLinkParser)
 * - EmailLinkSignInHandler.java:65-85 (same-device validation)
 * - EmailLinkSignInHandler.java:87-96 (anonymous user ID validation)
 * - EmailLinkSignInHandler.java:217 (DataStore cleanup after success)
 *
 * @see sendSignInLinkToEmail for sending the initial email link
 */
internal suspend fun FirebaseAuthUI.signInWithEmailLink(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    email: String,
    emailLink: String,
    existingUser: User? = null,
) {
    try {
        updateAuthState(AuthState.Loading("Signing in with email link..."))

        // Validate link format
        if (!auth.isSignInWithEmailLink(emailLink)) {
            return updateAuthState(
                AuthState.Error(
                    AuthException.UnknownException("Invalid email link")
                )
            )
        }

        // Parses email link for session data and returns sessionId, anonymousUserId,
        // force same device flag etc.
        val parser = EmailLinkParser(emailLink)
        val sessionIdFromLink = parser.sessionId
        val anonymousUserIdFromLink = parser.anonymousUserId
        val oobCode = parser.oobCode
        val providerIdFromLink = parser.providerId
        val isEmailLinkForceSameDeviceEnabled = parser.forceSameDeviceBit

        // Retrieve stored session record from DataStore
        val sessionRecord = EmailLinkPersistenceManager.retrieveSessionRecord(context)
        val storedSessionId = sessionRecord?.sessionId

        // Validate same-device
        when (provider.isDifferentDevice(
            sessionIdFromLocal = storedSessionId,
            sessionIdFromLink = sessionIdFromLink
        )) {
            true -> {
                if (sessionIdFromLink.isNullOrEmpty()) {
                    return updateAuthState(
                        AuthState.Error(
                            AuthException.InvalidEmailLinkException()
                        )
                    )
                }

                if (isEmailLinkForceSameDeviceEnabled
                    || !anonymousUserIdFromLink.isNullOrEmpty()
                ) {
                    return updateAuthState(
                        AuthState.Error(
                            AuthException.EmailLinkWrongDeviceException()
                        )
                    )
                }

                val actionCodeResult = auth.checkActionCode(oobCode).await()
                if (actionCodeResult != null) {
                    if (providerIdFromLink.isNullOrEmpty()) {
                        return updateAuthState(
                            AuthState.Error(
                                AuthException.EmailLinkCrossDeviceLinkingException()
                            )
                        )
                    }

                    return updateAuthState(
                        AuthState.Error(
                            AuthException.EmailLinkPromptForEmailException()
                        )
                    )
                }
            }

            false -> {
                // Validate anonymous user ID matches
                if (!anonymousUserIdFromLink.isNullOrEmpty()) {
                    val currentUser = auth.currentUser
                    if (currentUser == null
                        || !currentUser.isAnonymous
                        || currentUser.uid != anonymousUserIdFromLink
                    ) {
                        return updateAuthState(
                            AuthState.Error(
                                AuthException
                                    .EmailLinkDifferentAnonymousUserException()
                            )
                        )
                    }
                }

                if (email.isEmpty()) {
                    return updateAuthState(
                        AuthState.Error(
                            AuthException.EmailMismatchException()
                        )
                    )
                }

                // Get credential for linking from session record (already retrieved earlier)
                val storedCredentialForLink = sessionRecord?.credentialForLinking

                if (storedCredentialForLink == null) {
                    // Normal Flow
                    // Create credential and sign in
                    val emailLinkCredential =
                        EmailAuthProvider.getCredentialWithLink(email, emailLink)
                    signInAndLinkWithCredential(config, emailLinkCredential)
                } else {
                    // Linking Flow
                    // Sign in with email link first, then link the social credential
                    val emailLinkCredential =
                        EmailAuthProvider.getCredentialWithLink(email, emailLink)

                    if (AuthProvider.canUpgradeAnonymous(config, auth)) {
                        // Like scratch auth, this is used to avoid losing the anonymous user state in
                        // the main auth instance
                        val clonedAuth = FirebaseAuth
                            .getInstance(FirebaseApp.getInstance(app.name))

                        // Safe Link
                        // Add the provider to the same account before triggering a merge failure.
                        val authResult = clonedAuth
                            .signInWithCredential(emailLinkCredential).await()
                        if (authResult?.user != null) {
                            val linkResult = authResult
                                .user?.linkWithCredential(emailLinkCredential)?.await()
                            if (linkResult?.user != null) {
                                // Update AuthState with a firebase auth merge failure
                                return updateAuthState(
                                    AuthState.MergeConflict(
                                        emailLinkCredential
                                    )
                                )
                            }
                        }
                    } else {
                        // Sign in with email link
                        val authResult = auth.signInWithCredential(emailLinkCredential).await()

                        // Link the social credential
                        authResult.user?.linkWithCredential(storedCredentialForLink)?.await()
                        AuthProvider.mergeProfile(
                            auth,
                            existingUser?.name,
                            existingUser?.photoUri
                        )
                    }
                }

                // Clear DataStore after success
                EmailLinkPersistenceManager.clear(context)
            }
        }
        updateAuthState(AuthState.Idle)
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