package com.firebase.ui.com.firebasei.ui.authimpl;

/**
 * Created by abehaskins on 11/3/15.
 */
public class FirebaseOAuthToken {
    public String token;
    public String secret;
    public String uid;
    public String provider;

    public FirebaseOAuthToken(String provider, String token) {
        this.provider = provider;
        this.token = token;
    }

    public FirebaseOAuthToken(String provider, String token, String secret, String uid) {
        this.provider = provider;
        this.token = token;
        this.secret = secret;
        this.uid = uid;
    }
}
