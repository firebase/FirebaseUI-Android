package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public interface Provider {
    /**
     * Retrieves the name of the IDP, for display on-screen.
     */
    String getName(Context context);

    String getProviderId();

    void startLogin(Activity activity);

    void onActivityResult(int requestCode, int resultCode, Intent data);
}
