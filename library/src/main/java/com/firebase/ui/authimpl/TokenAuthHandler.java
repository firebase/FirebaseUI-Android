package com.firebase.ui.authimpl;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

public interface TokenAuthHandler {
    void onSuccess(AuthData auth);
    void onUserError(FirebaseError err);
    void onProviderError(FirebaseError err);
}