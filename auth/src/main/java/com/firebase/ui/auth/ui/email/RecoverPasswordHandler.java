package com.firebase.ui.auth.ui.email;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.util.data.AuthViewModel;
import com.firebase.ui.auth.util.data.SingleLiveEvent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class RecoverPasswordHandler extends AuthViewModel {
    private Pair<String, Task<Void>> mCachedPasswordReset;
    private MutableLiveData<Task<String>> mPasswordResetListener = new SingleLiveEvent<>();

    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public LiveData<Task<String>> getPasswordResetListener() {
        return mPasswordResetListener;
    }

    public void startReset(final String email) {
        if (mCachedPasswordReset == null || !TextUtils.equals(email, mCachedPasswordReset.first)) {
            mCachedPasswordReset = Pair.create(email, mAuth.sendPasswordResetEmail(email));
        }

        mCachedPasswordReset.second.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mPasswordResetListener.setValue(task.isSuccessful() ?
                        Tasks.forResult(email) : Tasks.<String>forException(task.getException()));
            }
        });
    }
}
