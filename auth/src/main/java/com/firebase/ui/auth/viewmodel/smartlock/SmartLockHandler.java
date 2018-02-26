package com.firebase.ui.auth.viewmodel.smartlock;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.CredentialsUtil;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.PendingResolution;
import com.firebase.ui.auth.viewmodel.ResolutionCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel for initiating saves to the Credentials API (SmartLock).
 */
public class SmartLockHandler extends AuthViewModelBase {

    private static final String TAG = "SmartLockViewModel";

    private MutableLiveData<Resource<Void>> mResultLiveData = new MutableLiveData<>();

    public SmartLockHandler(Application application) {
        super(application);
    }

    /**
     * Observe the status of the save operation initiated by
     * {@link #saveCredentials(FirebaseUser, String, String)}.
     */
    public LiveData<Resource<Void>> getSaveOperation() {
        return mResultLiveData;
    }

    /**
     * Forward the result of a resolution from the Activity to the ViewModel.
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ResolutionCodes.RC_CRED_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                mResultLiveData.setValue(Resource.forVoidSuccess());
            } else {
                Log.e(TAG, "SAVE: Canceled by user.");
                FirebaseUiException exception = new FirebaseUiException(
                        ErrorCodes.UNKNOWN_ERROR, "Save canceled by user.");
                mResultLiveData.setValue(Resource.<Void>forFailure(exception));
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
                                @Nullable String accountType) {

        Credential credential = CredentialsUtil.buildCredential(firebaseUser, password, accountType);
        saveCredentials(credential);
    }

    /**
     * Initialize saving a credential.
     */
    public void saveCredentials(Credential credential) {
        if (!getArguments().enableCredentials) {
            mResultLiveData.setValue(Resource.forVoidSuccess());
            return;
        }

        mResultLiveData.setValue(Resource.<Void>forLoading());


        if (credential == null) {
            Exception exception = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR,
                    "Failed to build credential.");
            mResultLiveData.setValue(Resource.<Void>forFailure(exception));
            return;
        }

        getCredentialsClient().save(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mResultLiveData.setValue(Resource.forVoidSuccess());
                        } else if (task.getException() instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) task.getException();
                            setPendingResolution(new PendingResolution(rae.getResolution(), ResolutionCodes.RC_CRED_SAVE));
                        } else {
                            Log.w(TAG, "Non-resolvable exception: " + task.getException());

                            FirebaseUiException exception = new FirebaseUiException(
                                    ErrorCodes.UNKNOWN_ERROR,
                                    "Error when saving credential.",
                                    task.getException());
                            mResultLiveData.setValue(Resource.<Void>forFailure(exception));
                        }
                    }
                });
    }

}
