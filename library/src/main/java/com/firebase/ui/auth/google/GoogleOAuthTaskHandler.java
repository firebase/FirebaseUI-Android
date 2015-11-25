package com.firebase.ui.auth.google;

import com.firebase.ui.auth.core.FirebaseLoginError;

interface GoogleOAuthTaskHandler {
    public void onOAuthSuccess(String token);
    public void onOAuthFailure(FirebaseLoginError firebaseError);
}
