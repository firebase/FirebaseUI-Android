package com.firebase.ui.com.firebasei.ui.authimpl;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

/**
 * Created by deast on 9/25/15.
 */
public interface TokenAuthHandler {
    void onSuccess(AuthData auth);
    void onUserError(FirebaseError err);
    void onProviderError(FirebaseError err);
}