package com.firebase.ui.auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Error codes for failed sign-in attempts.
 */
public final class ErrorCodes {
    /**
     * An unknown error has occurred.
     */
    public static final int UNKNOWN_ERROR = 0;
    /**
     * Sign in failed due to lack of network connection.
     */
    public static final int NO_NETWORK = 1;
    /**
     * A required update to Play Services was cancelled by the user.
     */
    public static final int PLAY_SERVICES_UPDATE_CANCELLED = 2;
    /**
     * A sign-in operation couldn't be completed due to a developer error.
     */
    public static final int DEVELOPER_ERROR = 3;
    /**
     * An external sign-in provider error occurred.
     */
    public static final int PROVIDER_ERROR = 4;
    /**
     * Anonymous account linking failed.
     */
    public static final int ANONYMOUS_UPGRADE_MERGE_CONFLICT = 5;
    /**
     * Signing in with a different email in the WelcomeBackIdp flow or email link flow.
     */
    public static final int EMAIL_MISMATCH_ERROR = 6;
    /**
     * Attempting to sign in with an invalid email link.
     */
    public static final int INVALID_EMAIL_LINK_ERROR = 7;

    /**
     * Attempting to open an email link from a different device.
     */
    public static final int EMAIL_LINK_WRONG_DEVICE_ERROR = 8;

    /**
     * We need to prompt the user for their email.
     * */
    public static final int EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR = 9;

    /**
     * Cross device linking flow - we need to ask the user if they want to continue linking or
     * just sign in.
     * */
    public static final int EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR = 10;

    /**
     * Attempting to open an email link from the same device, with anonymous upgrade enabled,
     * but the underlying anonymous user has been changed.
     */
    public static final int EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR = 11;

    /**
     *  Attempting to auth with account that is currently disabled in the Firebase console.
     */
    public static final int ERROR_USER_DISABLED = 12;

    /**
     *  Recoverable error occurred during the Generic IDP flow.
     */
    public static final int ERROR_GENERIC_IDP_RECOVERABLE_ERROR = 13;

    private ErrorCodes() {
        throw new AssertionError("No instance for you!");
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static String toFriendlyMessage(@Code int code) {
        switch (code) {
            case UNKNOWN_ERROR:
                return "Unknown error";
            case NO_NETWORK:
                return "No internet connection";
            case PLAY_SERVICES_UPDATE_CANCELLED:
                return "Play Services update cancelled";
            case DEVELOPER_ERROR:
                return "Developer error";
            case PROVIDER_ERROR:
                return "Provider error";
            case ANONYMOUS_UPGRADE_MERGE_CONFLICT:
                return "User account merge conflict";
            case EMAIL_MISMATCH_ERROR:
                return "You are are attempting to sign in a different email than previously " +
                        "provided";
            case INVALID_EMAIL_LINK_ERROR:
                return "You are are attempting to sign in with an invalid email link";
            case EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR:
                return "Please enter your email to continue signing in";
            case EMAIL_LINK_WRONG_DEVICE_ERROR:
                return "You must open the email link on the same device.";
            case EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR:
                return "You must determine if you want to continue linking or complete the sign in";
            case EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR:
                return "The session associated with this sign-in request has either expired or " +
                        "was cleared";
            case ERROR_USER_DISABLED:
                return "The user account has been disabled by an administrator.";
            case ERROR_GENERIC_IDP_RECOVERABLE_ERROR:
                return "Generic IDP recoverable error.";
            default:
                throw new IllegalArgumentException("Unknown code: " + code);
        }
    }

    /**
     * Valid codes that can be returned from {@link FirebaseUiException#getErrorCode()}.
     */
    @IntDef({
            UNKNOWN_ERROR,
            NO_NETWORK,
            PLAY_SERVICES_UPDATE_CANCELLED,
            DEVELOPER_ERROR,
            PROVIDER_ERROR,
            ANONYMOUS_UPGRADE_MERGE_CONFLICT,
            EMAIL_MISMATCH_ERROR,
            INVALID_EMAIL_LINK_ERROR,
            EMAIL_LINK_WRONG_DEVICE_ERROR,
            EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR,
            EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR,
            EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR,
            ERROR_USER_DISABLED,
            ERROR_GENERIC_IDP_RECOVERABLE_ERROR
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Code {
    }
}
