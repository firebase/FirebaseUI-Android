package com.firebase.ui.auth.ui.email;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordHandler extends AuthViewModelBase {
    private final MutableLiveData<RecoverPasswordProgressState> mProgressLiveData =
            new MutableLiveData<>();

    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public MutableLiveData<RecoverPasswordProgressState> getProgressLiveData() {
        return mProgressLiveData;
    }

    public void startReset(final String email) {
        mProgressLiveData.setValue(
                new RecoverPasswordProgressState());
        getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        RecoverPasswordProgressState state = task.isSuccessful() ?
                                new RecoverPasswordProgressState(email)
                                : new RecoverPasswordProgressState(task.getException());
                        mProgressLiveData.setValue(state);
                    }
                });
    }
}
