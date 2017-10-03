package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;

import com.firebase.ui.auth.util.ActivityResultHandler;

public interface Provider extends ActivityResultHandler {
    /** Retrieves the name of the IDP, for display on-screen. */
    String getName(Context context);

    /** Retrieves the layout id of the button to inflate and/or set a click listener. */
    @LayoutRes
    int getButtonLayout();

    /** Start the login process for the IDP, e.g. show the Google sign-in activity. */
    void startLogin(Activity activity);
}
