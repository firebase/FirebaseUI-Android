package com.firebase.ui.auth;

/**
 * Error codes retrieved from {@link IdpResponse#getErrorCode()}.
 */
public final class ErrorCodes {
    /**
     * Sign in failed due to lack of network connection
     **/
    public static final int NO_NETWORK = 10;

    /**
     * An unknown error has occurred
     **/
    public static final int UNKNOWN_ERROR = 20;

    private ErrorCodes() {
        // no instance
    }
}
