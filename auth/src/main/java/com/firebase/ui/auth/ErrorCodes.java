package com.firebase.ui.auth;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Error codes for failed sign-in attempts.
 */
public final class ErrorCodes {
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
                    EMAIL_MISMATCH_ERROR
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Code {}

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
     * Signing in with a different email in the WelcomeBackIdp flow.
     */
    public static final int EMAIL_MISMATCH_ERROR = 6;


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
            default:
                throw new IllegalArgumentException("Unknown code: " + code);
        }
    }
}
