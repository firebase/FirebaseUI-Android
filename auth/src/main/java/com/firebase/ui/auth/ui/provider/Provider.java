package com.firebase.ui.auth.ui.provider;

import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.ui.HelperActivityBase;

public interface Provider {
    /** Retrieves the name of the IDP, for display on-screen. */
    @NonNull
    String getName();

    /** Retrieves the layout id of the button to inflate and/or set a click listener. */
    @LayoutRes
    int getButtonLayout();

    /** Start the login process for the IDP, e.g. show the Google sign-in activity. */
    void startLogin(@NonNull HelperActivityBase activity);

    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
}
