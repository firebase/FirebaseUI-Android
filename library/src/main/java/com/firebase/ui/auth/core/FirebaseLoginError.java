package com.firebase.ui.auth.core;

public class FirebaseLoginError {
    public String message;
    public int error;

    public FirebaseLoginError(int error, String message) {
        this.message = message;
        this.error = error;
    }

    public String toString() {
        return Integer.toString(error) + ": " + message;
    }

}
