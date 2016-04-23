package com.firebase.ui.auth.core;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

import java.util.Map;

public interface TokenAuthHandler {
    void onSignupSuccess(Map<String, Object> result);
    void onSignupUserError(FirebaseSignupError err);
    void onLoginSuccess(AuthData auth);
    void onLoginUserError(FirebaseLoginError err);
    void onProviderError(FirebaseLoginError err);
}