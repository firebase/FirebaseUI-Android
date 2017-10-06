package com.firebase.ui.auth.ui.provider;

import android.support.annotation.LayoutRes;

import com.firebase.ui.auth.ui.HelperActivityBase;

public interface Provider {
    /** Retrieves the layout id of the button to inflate and/or set a click listener. */
    @LayoutRes
    int getButtonLayout();

    /** Start the login process for the IDP, e.g. show the Google sign-in activity. */
    void startLogin(HelperActivityBase activity);
}
