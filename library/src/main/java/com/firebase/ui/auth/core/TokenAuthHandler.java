package com.firebase.ui.auth.core;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

public interface TokenAuthHandler {
    void onSuccess(AuthData auth);
    void onUserError(FirebaseLoginError err);
    void onProviderError(FirebaseLoginError err);
}