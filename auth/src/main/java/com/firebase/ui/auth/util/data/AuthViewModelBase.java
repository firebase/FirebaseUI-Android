package com.firebase.ui.auth.util.data;

import android.app.Application;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.util.ui.ViewModelBase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;

public class AuthViewModelBase extends ViewModelBase<FlowParameters> {
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;

    protected AuthViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void onCreate() {
        FirebaseApp app = FirebaseApp.getInstance(getArguments().appName);
        mAuth = FirebaseAuth.getInstance(app);
        mPhoneAuth = PhoneAuthProvider.getInstance(mAuth);
    }

    protected FirebaseAuth getAuth() {
        return mAuth;
    }

    protected PhoneAuthProvider getPhoneAuth() {
        return mPhoneAuth;
    }
}
