package com.firebase.ui.auth.util.data;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.firebase.ui.auth.util.ui.ViewModelBase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;

public class AuthViewModelBase extends ViewModelBase<FlowHolder> {
    protected static MutableLiveData<IdpResponse> SIGN_IN_LISTENER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    SIGN_IN_LISTENER = new AutoClearSingleLiveEvent<>(this);
                }
            });

    protected FlowHolder mFlowHolder;
    protected FirebaseAuth mAuth;
    protected PhoneAuthProvider mPhoneAuth;

    protected AuthViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowHolder args) {
        mFlowHolder = args;

        FirebaseApp app = FirebaseApp.getInstance(mFlowHolder.getParams().appName);
        mAuth = FirebaseAuth.getInstance(app);
        mPhoneAuth = PhoneAuthProvider.getInstance(mAuth);
    }
}
