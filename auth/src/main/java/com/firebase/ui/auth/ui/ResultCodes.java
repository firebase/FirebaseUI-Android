package com.firebase.ui.auth.ui;

import android.app.Activity;

import com.firebase.ui.auth.AuthUI;

/**
 * Result codes returned when using {@link AuthUI.SignInIntentBuilder#build()} with
 * {@code startActivityForResult}.
 */
public class ResultCodes {
    /**
     * Sign in succeeded
     **/
    public static final int OK = Activity.RESULT_OK;

    /**
     * User cancelled sign in
     **/
    public static final int CANCELED = Activity.RESULT_CANCELED;

    /**
     * Sign in failed due to lack of network connection
     **/
    public static final int NO_NETWORK = 10;

    /**
     * An unknown error has occurred
     **/
    public static final int UNKNOWN_ERROR = 20;
}
