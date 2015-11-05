package com.firebase.ui.com.firebasei.ui.authimpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by deast on 9/25/15.
 */
public interface TokenAuthHandler {
    void onTokenReceived(FirebaseOAuthToken token);
    void onCancelled();
    void onError(Exception ex);
}