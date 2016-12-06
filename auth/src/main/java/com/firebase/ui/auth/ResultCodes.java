package com.firebase.ui.auth;

import android.app.Activity;

/**
 * Result codes returned when using {@link AuthUI.SignInIntentBuilder#build()} with
 * {@code startActivityForResult}.
 */
public final class ResultCodes {
    private ResultCodes() {
        // We don't want people to initialize this class
    }

    /**
     * Sign in succeeded
     **/
    public static final int OK = Activity.RESULT_OK;

    /**
     * Sign in canceled by user
     **/
    public static final int CANCELED = Activity.RESULT_CANCELED;
}
