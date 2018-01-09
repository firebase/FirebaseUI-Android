package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.google.firebase.auth.AuthResult;

/**
 * TODO(samstern): Document
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackPasswordHandler extends AuthViewModelBase {

    private MutableLiveData<Resource<AuthResult>> mSignInLiveData = new MutableLiveData<>();

    protected WelcomeBackPasswordHandler(Application application) {
        super(application);
    }

    public void startSignIn(String email, String password) {
        mSignInLiveData.setValue(new Resource<AuthResult>());

        // TODO
        getAuth().signInWithEmailAndPassword(email, password);
    }

}
