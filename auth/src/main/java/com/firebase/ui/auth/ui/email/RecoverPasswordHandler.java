package com.firebase.ui.auth.ui.email;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordHandler extends AuthViewModelBase {
    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public void startReset(final String email) {
        getFlowHolder().getProgressLiveData().setValue(
                new RecoverPasswordProgressState());
        getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        RecoverPasswordProgressState state = task.isSuccessful() ?
                                new RecoverPasswordProgressState(email)
                                : new RecoverPasswordProgressState(task.getException());
                        getFlowHolder().getProgressLiveData().setValue(state);
                    }
                });
    }
}
