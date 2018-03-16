package com.firebase.ui.auth.util;

import android.support.annotation.RestrictTo;

import com.google.firebase.auth.FirebaseAuthException;

/**
 * List of all possible results of {@link FirebaseAuthException#getErrorCode()} and their meanings.
 *
 * This is a temporary band-aid until we have better documentation and exposure for these
 * error codes in the real Firebase Auth SDK.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum FirebaseAuthError {

    ERROR_INVALID_CUSTOM_TOKEN("The custom token format is incorrect. Please check the documentation."),

    ERROR_CUSTOM_TOKEN_MISMATCH("Invalid configuration. Ensure your app's SHA1 is correct in the Firebase console."),

    ERROR_INVALID_CREDENTIAL("The supplied auth credential is malformed or has expired."),

    ERROR_INVALID_EMAIL("The email address is badly formatted."),

    ERROR_WRONG_PASSWORD("The password is invalid or the user does not have a password."),

    ERROR_USER_MISMATCH("The supplied credentials do not correspond to the previously signed in user."),

    ERROR_REQUIRES_RECENT_LOGIN("This operation is sensitive and requires recent authentication. Log in again before retrying this request."),

    ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL("An account already exists with the same email address but different sign-in credentials. Sign in using a provider associated with this email address."),

    ERROR_EMAIL_ALREADY_IN_USE("The email address is already in use by another account."),

    ERROR_CREDENTIAL_ALREADY_IN_USE("This credential is already associated with a different user account."),

    ERROR_USER_DISABLED("The user account has been disabled by an administrator."),

    ERROR_USER_TOKEN_EXPIRED("The user's credential has expired. The user must sign in again."),

    ERROR_USER_NOT_FOUND("There is no user record corresponding to this identifier. The user may have been deleted."),

    ERROR_INVALID_USER_TOKEN("The user's credential is no longer valid. The user must sign in again."),

    ERROR_OPERATION_NOT_ALLOWED("This operation is not allowed. Enable the sign-in method in the Authentication tab of the Firebase console"),

    ERROR_TOO_MANY_REQUESTS("We have blocked all requests from this device due to unusual activity. Try again later."),

    ERROR_WEAK_PASSWORD("The given password is too weak, please choose a stronger password."),

    ERROR_EXPIRED_ACTION_CODE("The out of band code has expired."),

    ERROR_INVALID_ACTION_CODE("The out of band code is invalid. This can happen if the code is malformed, expired, or has already been used."),

    ERROR_INVALID_MESSAGE_PAYLOAD("The email template corresponding to this action contains invalid characters in its message. Please fix by going to the Auth email templates section in the Firebase Console."),

    ERROR_INVALID_RECIPIENT_EMAIL("The email corresponding to this action failed to send as the provided recipient email address is invalid."),

    ERROR_INVALID_SENDER("The email template corresponding to this action contains an invalid sender email or name. Please fix by going to the Auth email templates section in the Firebase Console."),

    ERROR_MISSING_EMAIL("An email address must be provided."),

    ERROR_MISSING_PASSWORD("A password must be provided."),

    ERROR_MISSING_PHONE_NUMBER("To send verification codes, provide a phone number for the recipient."),

    ERROR_INVALID_PHONE_NUMBER("The format of the phone number provided is incorrect. Please enter the phone number in a format that can be parsed into E.164 format. E.164 phone numbers are written in the format [+][country code][subscriber number including area code]."),

    ERROR_MISSING_VERIFICATION_CODE("The phone auth credential was created with an empty sms verification code"),

    ERROR_INVALID_VERIFICATION_CODE("The sms verification code used to create the phone auth credential is invalid. Please resend the verification code sms and be sure use the verification code provided by the user."),

    ERROR_MISSING_VERIFICATION_ID("The phone auth credential was created with an empty verification ID"),

    ERROR_INVALID_VERIFICATION_ID("The verification ID used to create the phone auth credential is invalid."),

    ERROR_RETRY_PHONE_AUTH("An error occurred during authentication using the PhoneAuthCredential. Please retry authentication."),

    ERROR_SESSION_EXPIRED("The sms code has expired. Please re-send the verification code to try again."),

    ERROR_QUOTA_EXCEEDED("The sms quota for this project has been exceeded."),

    ERROR_APP_NOT_AUTHORIZED("This app is not authorized to use Firebase Authentication. Please verify that the correct package name and SHA-1 are configured in the Firebase Console."),

    ERROR_API_NOT_AVAILABLE("The API that you are calling is not available on devices without Google Play Services."),

    ERROR_UNKNOWN("An unknown error occurred.");

    /**
     * Get an {@link FirebaseAuthError} from an exception, returning {@link #ERROR_UNKNOWN} as
     * a default.
     */
    public static FirebaseAuthError fromException(FirebaseAuthException ex) {
        try {
            return FirebaseAuthError.valueOf(ex.getErrorCode());
        } catch (IllegalArgumentException e) {
            return FirebaseAuthError.ERROR_UNKNOWN;
        }
    }

    private final String description;

    FirebaseAuthError(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
