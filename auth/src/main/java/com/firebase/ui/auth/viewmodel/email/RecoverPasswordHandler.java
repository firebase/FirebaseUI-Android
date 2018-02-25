package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordHandler extends AuthViewModelBase {
    private final MutableLiveData<Resource<String>> mProgressLiveData =
            new MutableLiveData<>();

    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public LiveData<Resource<String>> getProgressLiveData() {
        return mProgressLiveData;
    }

    public void startReset(final String email) {
        mProgressLiveData.setValue(Resource.<String>forLoading());
        getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Resource<String> state = task.isSuccessful()
                                ? Resource.forSuccess(email)
                                : Resource.<String>forFailure(task.getException());
                        mProgressLiveData.setValue(state);
                    }
                });
    }
}
