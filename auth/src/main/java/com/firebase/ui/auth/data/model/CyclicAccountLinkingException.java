package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;

/**
 * Represents a sign-in failure that requires manual data transfer on the client's part.
 * <p>
 * The most common case is 3 way account linking which happens when: <p>1. The user is already
 * logged-in before starting the auth flow <p>2. The user already has another account that will
 * cause linking failures <p>3. The user tries to log in with a third account
 * <p>
 * For example, the user is logged-in anonymously, has a Google account, and tries to log in with
 * Facebook.
 */
public class CyclicAccountLinkingException extends FirebaseUiException {
    public CyclicAccountLinkingException(@NonNull String detailMessage) {
        super(ErrorCodes.LINK_FAILURE, detailMessage);
    }
}
