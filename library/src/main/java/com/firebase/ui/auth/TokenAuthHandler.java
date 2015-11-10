package com.firebase.ui.auth;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

public interface TokenAuthHandler {
    void onSuccess(AuthData auth);
    void onUserError(FirebaseError err);
    void onProviderError(FirebaseError err);
}