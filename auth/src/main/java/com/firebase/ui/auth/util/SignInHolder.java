package com.firebase.ui.auth.util;

import android.os.Bundle;
import android.util.Pair;

import com.firebase.ui.auth.ui.FlowParameters;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;

public class SignInHolder extends ViewModelBase<Pair<FlowParameters, Bundle>> {
    private FirebaseApp mApp;

    @Override
    protected void onCreate(Pair<FlowParameters, Bundle> args) {
        mApp = FirebaseApp.getInstance(args.first.appName);
    }

    public FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(mApp);
    }

    public PhoneAuthProvider getPhoneAuthProvider() {
        return PhoneAuthProvider.getInstance(getFirebaseAuth());
    }
}
