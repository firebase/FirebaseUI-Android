package com.firebase.ui.auth.viewmodel;

import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RequestCodes {
    /** Request code for saving a credential. */
    public static final int CRED_SAVE = 100;

    /** Request code for retrieving a credential. */
    public static final int CRED_HINT = 101;

    /** Request code for starting a credential save flow. */
    public static final int CRED_SAVE_FLOW = 102;

    /** Request code for starting an IDP welcome back flow. */
    public static final int WELCOME_BACK_IDP_FLOW = 103;

    /** Request code for starting an email welcome back flow. */
    public static final int WELCOME_BACK_EMAIL_FLOW = 104;

    /** Request code for starting a user pickable provider flow. */
    public static final int AUTH_PICKER_FLOW = 105;

    /** Request code for starting a fresh email flow. */
    public static final int EMAIL_FLOW = 106;

    /** Request code for starting a fresh phone flow. */
    public static final int PHONE_FLOW = 107;

    /** Request code for starting an account linking flow. */
    public static final int ACCOUNT_LINK_FLOW = 108;

    /** Request code for starting a specific provider's login flow. */
    public static final int PROVIDER_FLOW = 109;

    /** Request code for retrieving a Google credential. */
    public static final int GOOGLE_PROVIDER = 110;

    /** Request code for retrieving a GitHub credential. */
    public static final int GITHUB_PROVIDER = 111;

    private RequestCodes() {
        throw new AssertionError("No instance for you!");
    }
}
