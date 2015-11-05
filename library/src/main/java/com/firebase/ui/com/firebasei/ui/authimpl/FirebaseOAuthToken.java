package com.firebase.ui.com.firebasei.ui.authimpl;

public class FirebaseOAuthToken {
    public String token;
    public String secret;
    public String uid;
    public String provider;
    public int mode;

    public static final int SIMPLE = 1;
    public static final int COMPLEX = 2;

    public FirebaseOAuthToken(String provider, String token) {
        this.provider = provider;
        this.token = token;
        this.mode = SIMPLE;
    }

    public FirebaseOAuthToken(String provider, String token, String secret, String uid) {
        this.provider = provider;
        this.token = token;
        this.secret = secret;
        this.uid = uid;
        this.mode = COMPLEX;
    }
}
