package com.firebase.ui.auth.google;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

public interface GoogleTokenHandler {
    void onTokenReceived(String token);
    void onUserRecoverableAuthException(UserRecoverableAuthException ex);
    void onGoogleAuthException(GoogleAuthException ex);
    void onIOException(IOException ex);
}