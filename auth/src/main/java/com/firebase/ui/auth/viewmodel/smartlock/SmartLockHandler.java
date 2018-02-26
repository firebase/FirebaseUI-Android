package com.firebase.ui.auth.viewmodel.smartlock;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
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
import com.firebase.ui.auth.viewmodel.SingleLiveEvent;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel for initiating saves to the Credentials API (SmartLock).
 */
public class SmartLockHandler extends AuthViewModelBase<Void> {
    private static final String TAG = "SmartLockViewModel";

    private SingleLiveEvent<PendingResolution> mPendingResolution = new SingleLiveEvent<>();

    public SmartLockHandler(Application application) {
        super(application);
    }

    /**
     * Get an observable stream of {@link PendingIntent} resolutions requested by the ViewModel.
     * <p>
     * Make sure to call {@link #onActivityResult(int, int, Intent)} for all activity results after
     * firing these pending intents.
     */
    public LiveData<PendingResolution> getPendingResolution() {
        return mPendingResolution;
    }

    /**
     * Forward the result of a resolution from the Activity to the ViewModel.
     */
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == ResolutionCodes.RC_CRED_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Resource.forVoidSuccess());
            } else {
                Log.e(TAG, "SAVE: Canceled by user.");
                FirebaseUiException exception = new FirebaseUiException(
                        ErrorCodes.UNKNOWN_ERROR, "Save canceled by user.");
                setResult(Resource.<Void>forFailure(exception));
            }
        }
    }

    /** @see #saveCredentials(Credential) */
    public void saveCredentials(FirebaseUser firebaseUser,
                                @Nullable String password,
                                @Nullable String accountType) {
        saveCredentials(CredentialsUtil.buildCredential(firebaseUser, password, accountType));
    }

    /** Initialize saving a credential. */
    public void saveCredentials(Credential credential) {
        if (!getArguments().enableCredentials) {
            setResult(Resource.forVoidSuccess());
            return;
        }

        setResult(Resource.<Void>forLoading());


        if (credential == null) {
            Exception exception = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR,
                    "Failed to build credential.");
            setResult(Resource.<Void>forFailure(exception));
            return;
        }

        getCredentialsClient().save(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.forVoidSuccess());
                        } else if (task.getException() instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) task.getException();
                            mPendingResolution.setValue(new PendingResolution(
                                    rae.getResolution(), ResolutionCodes.RC_CRED_SAVE));
                        } else {
                            Log.w(TAG, "Non-resolvable exception: " + task.getException());

                            FirebaseUiException exception = new FirebaseUiException(
                                    ErrorCodes.UNKNOWN_ERROR,
                                    "Error when saving credential.",
                                    task.getException());
                            setResult(Resource.<Void>forFailure(exception));
                        }
                    }
                });
    }
}
