package com.firebase.ui.auth.core;

public class FirebaseSignupError {
    public String message;
    public FirebaseResponse error;

    public FirebaseSignupError(FirebaseResponse error, String message) {
        this.message = message;
        this.error = error;
    }

    public String toString() {
        return error.toString() + ": " + message;
    }
}