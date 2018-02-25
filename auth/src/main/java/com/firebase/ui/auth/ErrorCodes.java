package com.firebase.ui.auth;

import com.firebase.ui.auth.data.model.CyclicAccountLinkingException;

/**
 * Error codes retrieved from {@link IdpResponse#getErrorCode()}.
 */
public final class ErrorCodes {
    /**
     * Sign in failed due to lack of network connection
     */
    public static final int NO_NETWORK = 10;

    /**
     * An unknown error has occurred
     */
    public static final int UNKNOWN_ERROR = 20;

    /**
     * An error occurred link the user's accounts.
     *
     * @see CyclicAccountLinkingException
     **/
    public static final int LINK_FAILURE = 40;

    /**
     * An error occurred connecting to Google Play Services.
     */
    public static final int PLAY_SERVICES_ERROR = 50;

    /**
     * An external sign-in provider error occurred.
     */
    public static final int PROVIDER_ERROR = 50;

    /**
     * An error occurred due to a developer misconfiguration.
     */
    public static final int DEVELOPER_ERROR = 60;

    private ErrorCodes() {
        // no instance
    }
}
