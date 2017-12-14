package com.firebase.ui.auth.ui.email;

import android.app.Application;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class RecoverPasswordHandler extends AuthViewModelBase {
    private Pair<String, Task<Void>> mCachedPasswordReset;

    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public void startReset(final String email) {
        if (mCachedPasswordReset == null || !TextUtils.equals(email, mCachedPasswordReset.first)) {
            mCachedPasswordReset = Pair.create(email, getAuth().sendPasswordResetEmail(email));
        }

        getFlowHolder().getProgressLiveData().setValue(
                new RecoverPasswordProgressState(false));
        mCachedPasswordReset.second.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                getFlowHolder().getProgressLiveData().setValue(
                        new RecoverPasswordProgressState(true, email, task.getException()));
            }
        });
    }
}
