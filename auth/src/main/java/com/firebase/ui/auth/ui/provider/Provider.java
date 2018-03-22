package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.HelperActivityBase;

/**
 * Middleman to communicate between context related classes such as activities and the provider sign
 * in handlers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Provider {
    LiveData<IdpResponse> getResponseListener();

    /** Retrieves the name of the IDP, for display on-screen. */
    @StringRes
    int getNameRes();

    /** Retrieves the layout id of the button to inflate and/or set a click listener. */
    @LayoutRes
    int getButtonLayout();

    /** Start the login process for the IDP, e.g. show the Google sign-in activity. */
    void startSignIn(@NonNull HelperActivityBase activity);

    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
}
