package com.firebase.ui.auth.viewmodel;

import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RequestCodes {

    /**
     * Request code for saving a credential.
     */
    public static final int CRED_SAVE = 100;

    /**
     * Request code for retrieving a credential.
     */
    public static final int CRED_HINT = 101;

    /**
     * Request code for starting an IDP welcome back flow.
     */
    public static final int WELCOME_BACK_IDP_FLOW = 102;

    /**
     * Request code for starting an email welcome back flow.
     */
    public static final int WELCOME_BACK_EMAIL_FLOW = 103;

    /**
     * Request code for starting a user pickable provider flow.
     */
    public static final int AUTH_PICKER_FLOW = 104;

    /**
     * Request code for starting a fresh email flow.
     */
    public static final int RC_EMAIL_FLOW = 105;

    /**
     * Request code for starting a fresh phone flow.
     */
    public static final int RC_PHONE_FLOW = 106;

    /**
     * Request code for starting an account linking flow.
     */
    public static final int ACCOUNT_LINK_FLOW = 107;

    /**
     * Request code for starting a specific provider's login flow.
     */
    public static final int PROVIDER_FLOW = 108;

    /**
     * Request code for retrieving a Google credential.
     */
    public static final int GOOGLE_PROVIDER = 109;

    /**
     * Request code for checking if a valid version of Play Services exists.
     */
    public static final int PLAY_SERVICES_CHECK = 110;

}
