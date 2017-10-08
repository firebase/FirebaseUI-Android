package com.firebase.ui.auth.ui.email;

import android.app.Application;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.util.data.AuthViewModel;
import com.google.android.gms.tasks.Task;

public class RecoverPasswordHandler extends AuthViewModel {
    private Pair<String, Task<Void>> mCachedPasswordReset;

    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public Task<Void> getPasswordResetTask(String email) {
        if (mCachedPasswordReset == null || !TextUtils.equals(email, mCachedPasswordReset.first)) {
            mCachedPasswordReset = Pair.create(email, mAuth.sendPasswordResetEmail(email));
        }

        return mCachedPasswordReset.second;
    }
}
