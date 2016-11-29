package com.firebase.ui.auth.ui;

import com.firebase.ui.auth.AuthUI;

/**
 * Result codes returned when using {@link AuthUI.SignInIntentBuilder#build()} with
 * {@code startActivityForResult}.
 */
@Deprecated
public class ResultCodes {

    /**
     * Sign in failed due to lack of network connection
     *
     * @deprecated Please use {@link com.firebase.ui.auth.ErrorCodes#NO_NETWORK}
     **/
    @Deprecated
    public static final int RESULT_NO_NETWORK = com.firebase.ui.auth.ErrorCodes.NO_NETWORK;

}
