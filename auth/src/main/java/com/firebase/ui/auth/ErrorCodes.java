package com.firebase.ui.auth;

import android.support.annotation.IntDef;

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
                    DEVELOPER_ERROR
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

    private ErrorCodes() {
        throw new AssertionError("No instance for you!");
    }
}
