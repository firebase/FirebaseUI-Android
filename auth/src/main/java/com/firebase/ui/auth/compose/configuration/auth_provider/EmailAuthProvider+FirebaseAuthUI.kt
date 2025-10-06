package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import android.net.Uri
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
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
 * Holds credential information for account linking with email link sign-in.
 *
 * When a user tries to sign in with a social provider (Google, Facebook, etc.) but an
 * email link account exists with that email, this data is used to link the accounts
 * after email link authentication completes.
 *
 * @property providerType The provider ID (e.g., "google.com", "facebook.com")
 * @property idToken The ID token from the provider (required for Google, optional for Facebook)
 * @property accessToken The access token from the provider (required for Facebook, optional for Google)
 */
internal class CredentialForLinking(
    val providerType: String,
    val idToken: String?,
    val accessToken: String?
)

/**
 * Creates an email/password account or links the credential to an anonymous user.
 *
 * Mirrors the legacy email sign-up handler: validates password strength, validates custom
 * password rules, checks if new accounts are allowed, chooses between
 * `createUserWithEmailAndPassword` and `linkWithCredential`, merges the supplied display name
 * into the Firebase profile, and emits [AuthState.MergeConflict] when anonymous upgrade
 * encounters an existing account for the email.
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
 * } catch (e: AuthException) {
 *     // Check if AuthState.MergeConflict was emitted
 *     // This means email already exists - show merge conflict UI
 * }
 * ```
 *
 * **Old library reference:**
 * - EmailProviderResponseHandler.java:42-84 (startSignIn implementation)
 * - AuthOperationManager.java:64-74 (createOrLinkUserWithEmailAndPassword)
 * - RegisterEmailFragment.java:270-287 (validation and triggering sign-up)
 * - ProfileMerger.java:34-56 (profile merging after sign-up)
 */
internal suspend fun FirebaseAuthUI.createOrLinkUserWithEmailAndPassword(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    name: String?,
    email: String,
    password: String
): AuthResult? {
    val canUpgrade = AuthProvider.canUpgradeAnonymous(config, auth)
    val pendingCredential =
        if (canUpgrade) EmailAuthProvider.getCredential(email, password) else null

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
                AuthProvider.mergeProfile(auth, name, null)
            }
        }
        updateAuthState(AuthState.Idle)
        return result
    } catch (e: FirebaseAuthUserCollisionException) {
        val authException = AuthException.from(e)
        if (canUpgrade && pendingCredential != null) {
            // Anonymous upgrade collision: emit merge conflict state
            updateAuthState(AuthState.MergeConflict(pendingCredential))
        } else {
            // Non-upgrade collision: user exists with this email
            // TODO: Fetch top provider and emit AuthState.RequiresSignIn(provider, email)
            // For now, just emit the error
            updateAuthState(AuthState.Error(authException))
        }
        throw authException
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
 * scenarios, it validates credentials in a scratch auth instance before emitting a merge
 * conflict state.
 *
 * **Flow:**
 * 1. If anonymous upgrade:
 *    - Create scratch auth instance to validate credential
 *    - If linking social provider: sign in with email, then link social credential (safe link)
 *    - Otherwise: just validate email credential
 *    - Emit [AuthState.MergeConflict] after successful validation
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
 * @throws AuthException.UserDisabledException if the user account is disabled
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
 * } catch (e: AuthException) {
 *     // AuthState.MergeConflict emitted
 *     // UI shows merge conflict resolution screen
 * }
 * ```
 *
 * **Old library reference:**
 * - WelcomeBackPasswordHandler.java:45-118 (startSignIn implementation)
 * - AuthOperationManager.java:76-84 (signInAndLinkWithCredential)
 * - AuthOperationManager.java:97-108 (safeLink for social providers)
 * - AuthOperationManager.java:92-95 (validateCredential for email-only)
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
        return if (AuthProvider.canUpgradeAnonymous(config, auth)) {
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
                        // Emit merge conflict after successful validation
                        updateAuthState(AuthState.MergeConflict(credentialToValidate))
                    }
            } else {
                // Just validate the email credential
                // No linking for non-federated IDPs
                authExplicitlyForValidation
                    .signInWithCredential(credentialToValidate).await()
                    .also {
                        // Emit merge conflict after successful validation
                        // Merge failure occurs because account exists and user is anonymous
                        updateAuthState(AuthState.MergeConflict(credentialToValidate))
                    }
            }
        } else {
            // Normal sign-in
            auth.signInWithEmailAndPassword(email, password).await()
                .also { result ->
                    // If there's a credential to link, link it after sign-in
                    if (credentialForLinking != null) {
                        return result.user?.linkWithCredential(credentialForLinking)?.await()
                            .also { linkResult ->
                                // Merge profile from social provider
                                linkResult?.user?.let { user ->
                                    AuthProvider.mergeProfile(
                                        auth,
                                        user.displayName,
                                        user.photoUrl
                                    )
                                }
                            }
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
 * 5. Handle collision exceptions by emitting [AuthState.MergeConflict]
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
 * } catch (e: FirebaseAuthUserCollisionException) {
 *     // Phone number already exists on another account
 *     // AuthState.MergeConflict emitted with updatedCredential
 *     // UI can show merge conflict resolution screen
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
 *
 * **Old library reference:**
 * - AuthOperationManager.java:76-84 (signInAndLinkWithCredential implementation)
 * - ProfileMerger.java:34-56 (profile merging after sign-in)
 * - SocialProviderResponseHandler.java:69-74 (usage with profile merge)
 * - PhoneProviderResponseHandler.java:38-40 (usage for phone auth)
 * - EmailLinkSignInHandler.java:217 (usage for email link)
 */
internal suspend fun FirebaseAuthUI.signInAndLinkWithCredential(
    config: AuthUIConfiguration,
    credential: AuthCredential,
    displayName: String? = null,
    photoUrl: Uri? = null
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in user..."))
        return if (AuthProvider.canUpgradeAnonymous(config, auth)) {
            auth.currentUser?.linkWithCredential(credential)?.await()
        } else {
            auth.signInWithCredential(credential).await()
        }.also { result ->
            // Merge profile information from the provider
            result?.user?.let {
                AuthProvider.mergeProfile(auth, displayName, photoUrl)
            }
            updateAuthState(AuthState.Idle)
        }
    } catch (e: FirebaseAuthUserCollisionException) {
        // Special handling for collision exceptions
        val authException = AuthException.from(e)

        if (AuthProvider.canUpgradeAnonymous(config, auth)) {
            // Anonymous upgrade collision: emit merge conflict with updated credential
            val updatedCredential = e.updatedCredential
            if (updatedCredential != null) {
                updateAuthState(AuthState.MergeConflict(updatedCredential))
            } else {
                updateAuthState(AuthState.Error(authException))
            }
        } else {
            // Non-anonymous collision: could be same email different provider
            // TODO: Fetch providers and emit AuthState.RequiresSignIn
            updateAuthState(AuthState.Error(authException))
        }
        throw authException
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
 * and security. Optionally supports account linking when a user tries to sign in with
 * a social provider but an email link account exists.
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
 * account already exists with that email, you can link the accounts by:
 * 1. Catching the [FirebaseAuthUserCollisionException] from the social sign-in attempt
 * 2. Calling this method with [credentialForLinking] containing the social provider tokens
 * 3. When [signInWithEmailLink] completes, it automatically retrieves and links the saved credential
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
 * @param credentialForLinking Optional credential linking data. If provided, this credential
 *        will be automatically linked after email link sign-in completes. Pass null for basic
 *        email link sign-in without account linking.
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
 * **Example 2: Complete account linking flow (Google → Email Link)**
 * ```kotlin
 * // Step 1: User tries to sign in with Google
 * try {
 *     val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
 *     val googleIdToken = googleAccount?.idToken
 *     val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
 *
 *     firebaseAuthUI.signInAndLinkWithCredential(
 *         config = authUIConfig,
 *         credential = googleCredential
 *     )
 * } catch (e: FirebaseAuthUserCollisionException) {
 *     // Email already exists with Email Link provider
 *
 *     // Step 2: Send email link with credential for linking
 *     firebaseAuthUI.sendSignInLinkToEmail(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         email = email,
 *         credentialForLinking = CredentialForLinking(
 *             providerType = "google.com",
 *             idToken = googleIdToken,  // From GoogleSignInAccount
 *             accessToken = null
 *         )
 *     )
 *
 *     // Step 3: Show "Check your email" UI
 * }
 *
 * // Step 4: User clicks email link → App opens
 * // (In your deep link handling Activity)
 * val emailLink = intent.data.toString()
 * firebaseAuthUI.signInWithEmailLink(
 *     context = context,
 *     config = authUIConfig,
 *     provider = emailProvider,
 *     email = email,
 *     emailLink = emailLink
 * )
 * // signInWithEmailLink automatically:
 * // 1. Signs in with email link
 * // 2. Retrieves the saved Google credential from DataStore
 * // 3. Links the Google credential to the email link account
 * // 4. User is now signed in with both Email Link AND Google linked
 * ```
 *
 * **Example 3: Anonymous user upgrade**
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
 *
 * **Old library reference:**
 * - EmailLinkSendEmailHandler.java:26-55 (complete implementation)
 * - EmailLinkSendEmailHandler.java:38-39 (session ID generation)
 * - EmailLinkSendEmailHandler.java:47-48 (DataStore persistence)
 * - EmailActivity.java:92-93 (saving credential for linking before sending email)
 *
 * @see signInWithEmailLink
 * @see EmailLinkPersistenceManager.saveCredentialForLinking
 * @see com.google.firebase.auth.FirebaseAuth.sendSignInLinkToEmail
 */
internal suspend fun FirebaseAuthUI.sendSignInLinkToEmail(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    email: String,
    credentialForLinking: CredentialForLinking? = null
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

        // If credential provided, save it for linking after email link sign-in
        if (credentialForLinking != null) {
            EmailLinkPersistenceManager.saveCredentialForLinking(
                context = context,
                providerType = credentialForLinking.providerType,
                idToken = credentialForLinking.idToken,
                accessToken = credentialForLinking.accessToken
            )
        }

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
): AuthResult {
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
            provider.handleCrossDeviceEmailLink(
                auth = auth,
                sessionIdFromLink = sessionIdFromLink,
                anonymousUserIdFromLink = anonymousUserIdFromLink,
                isEmailLinkForceSameDeviceEnabled = isEmailLinkForceSameDeviceEnabled,
                oobCode = oobCode,
                providerIdFromLink = providerIdFromLink
            )
        }

        // Validate anonymous user ID matches (same-device flow)
        if (!anonymousUserIdFromLink.isNullOrEmpty()) {
            val currentUser = auth.currentUser
            if (currentUser == null || !currentUser.isAnonymous || currentUser.uid != anonymousUserIdFromLink) {
                throw AuthException.EmailLinkDifferentAnonymousUserException()
            }
        }

        // Get credential for linking from session record
        val storedCredentialForLink = sessionRecord?.credentialForLinking
        val emailLinkCredential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        val result = if (storedCredentialForLink == null) {
            // Normal Flow: Just sign in with email link
            signInAndLinkWithCredential(config, emailLinkCredential)
                ?: throw AuthException.UnknownException("Sign in failed")
        } else {
            // Linking Flow: Sign in with email link, then link the social credential
            provider.handleEmailLinkWithSocialLinking(
                context = context,
                config = config,
                auth = auth,
                emailLinkCredential = emailLinkCredential,
                storedCredentialForLink = storedCredentialForLink,
                updateAuthState = ::updateAuthState
            )
        }

        // Clear DataStore after success
        EmailLinkPersistenceManager.clear(context)

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
 * 3. User clicks link in email to reset password
 * 4. User is redirected to Firebase-hosted password reset page (or custom URL if configured)
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
 * @return The email address that the reset link was sent to (useful for confirmation UI)
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
 *     val email = firebaseAuthUI.sendPasswordResetEmail(
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
 * val email = firebaseAuthUI.sendPasswordResetEmail(
 *     email = "user@example.com",
 *     actionCodeSettings = actionCodeSettings
 * )
 * // User receives email with custom continue URL
 * ```
 *
 * **Old library reference:**
 * - RecoverPasswordHandler.java:21-33 (startReset method)
 * - RecoverPasswordActivity.java:131-133 (resetPassword caller)
 * - RecoverPasswordActivity.java:76-91 (error handling for invalid user/credentials)
 *
 * @see com.google.firebase.auth.ActionCodeSettings
 * @since 10.0.0
 */
internal suspend fun FirebaseAuthUI.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings? = null
): String {
    try {
        updateAuthState(AuthState.Loading("Sending password reset email..."))

        if (actionCodeSettings != null) {
            auth.sendPasswordResetEmail(email, actionCodeSettings).await()
        } else {
            auth.sendPasswordResetEmail(email).await()
        }

        updateAuthState(AuthState.Idle)
        return email
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
