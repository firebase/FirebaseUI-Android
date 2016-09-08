package com.firebase.ui.auth.test_helpers;

import android.app.Activity;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Collection;

/**
 * Shadow for Facebook SDK {@link LoginManager}.
 */
@Implements(LoginManager.class)
public class LoginManagerShadow {

    private FacebookCallback<LoginResult> mCallback;

    @Implementation
    public void registerCallback(final CallbackManager callbackManager,
                                 final FacebookCallback<LoginResult> callback) {
        mCallback = callback;
    }

    @Implementation
    public void logInWithReadPermissions(Activity activity, Collection<String> permissions) {
        // Check for minimum permission set
        if (!(permissions.contains("email") && permissions.contains("public_profile"))) {
            throw new IllegalArgumentException("Facebook permissions must contain email and " +
                    "public_profile.");
        }

        // Call back with success
        mCallback.onSuccess(null);
    }

}
