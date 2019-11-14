package com.firebase.ui.auth.viewmodel;

import androidx.annotation.RestrictTo;

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

    /** Request code for starting an email link welcome back flow. */
    public static final int WELCOME_BACK_EMAIL_LINK_FLOW = 112;

    /** Request code for recovering from opening an email link from the wrong device */
    public static final int EMAIL_LINK_WRONG_DEVICE_FLOW = 113;

    /** Request code for recovering from opening an invalid email link */
    public static final int EMAIL_LINK_INVALID_LINK_FLOW = 114;

    /** Request code for prompting the user to enter their email to finish the sign in */
    public static final int EMAIL_LINK_PROMPT_FOR_EMAIL_FLOW = 115;

    /** Request code for prompting the user to enter their email to finish the sign in */
    public static final int EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW = 116;

    /** Request code for starter a generic IDP sign-in flow */
    public static final int GENERIC_IDP_SIGN_IN_FLOW = 117;

    private RequestCodes() {
        throw new AssertionError("No instance for you!");
    }
}
