package com.firebase.ui.auth.data.remote;

import android.app.Application;

import com.firebase.ui.auth.util.data.AuthViewModel;

public class SignInKickstarter extends AuthViewModel {
    public SignInKickstarter(Application application) {
        super(application);
    }

    public void start() {
        throw new IllegalStateException("TODO");
    }
}
