package com.firebase.ui.com.firebasei.ui.authimpl;

/**
 * Created by deast on 9/25/15.
 */
public interface TokenAuthHandler {
    void onTokenReceived(String token);
    void onCancelled();
    void onError(Exception ex);
}