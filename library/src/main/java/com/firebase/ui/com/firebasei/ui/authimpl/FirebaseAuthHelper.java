package com.firebase.ui.com.firebasei.ui.authimpl;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

/**
 * Created by abehaskins on 11/4/15.
 */
public interface FirebaseAuthHelper {
    String getProviderName();
    void login();
    void logout();
}
