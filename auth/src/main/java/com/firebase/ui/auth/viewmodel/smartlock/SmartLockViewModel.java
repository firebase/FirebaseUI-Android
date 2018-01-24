package com.firebase.ui.auth.viewmodel.smartlock;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.CredentialsUtil;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.PendingResolution;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel for initiating saves to the Credentials API (SmartLock).
 */
public class SmartLockViewModel extends AuthViewModelBase {

    private static final String TAG = "SmartLockViewModel";
    private static final int RC_SAVE = 100;

    private MutableLiveData<Resource<IdpResponse>> mResultLiveData = new MutableLiveData<>();

    private IdpResponse mIdpResponse;

    public SmartLockViewModel(Application application) {
        super(application);
    }

    /**
     * Observe the status of the save operation initiated by
     * {@link #saveCredentials(FirebaseUser, String, IdpResponse)}.
     */
    public LiveData<Resource<IdpResponse>> getSaveOperation() {
        return mResultLiveData;
    }

    /**
     * Forward the result of a resolution from the Activity to the ViewModel.
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                setSuccessValue();
            } else {
                Log.e(TAG, "SAVE: Canceled by user.");
                setException(new Exception("Save canceled by user."));
            }

            return true;
        }

        return super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Initialize saving a credential. Progress of the operation can be observed in
     * {@link #getSaveOperation()}.
     */
    public void saveCredentials(FirebaseUser firebaseUser,
                                @Nullable String password,
                                @Nullable IdpResponse response) {

        mIdpResponse = response;

        if (!getArguments().enableCredentials) {
            setSuccessValue();
            return;
        }

        mResultLiveData.setValue(new Resource<IdpResponse>());

        final Credential credential = CredentialsUtil.buildCredential(
                firebaseUser.getEmail(),
                password,
                firebaseUser.getDisplayName(),
                firebaseUser.getPhotoUrl() == null ? null : firebaseUser.getPhotoUrl().toString(),
                response);

        getCredentialsClient().save(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            setSuccessValue();
                        } else if (task.getException() instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) task.getException();
                            setPendingResolution(new PendingResolution(rae.getResolution(), RC_SAVE));
                        } else {
                            Log.w(TAG, "Non-resolvable exception: " + task.getException());
                            setException(task.getException());
                        }
                    }
                });
    }

    private void setSuccessValue() {
        mResultLiveData.setValue(new Resource<>(mIdpResponse));
    }

    private void setException(Exception e) {
        mResultLiveData.setValue(new Resource<IdpResponse>(e));
    }

    /**
     * TODO: This stinks
     */
    public IdpResponse getIdpResponse() {
        return mIdpResponse;
    }
}
