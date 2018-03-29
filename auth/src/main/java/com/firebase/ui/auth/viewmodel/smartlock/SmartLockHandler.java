package com.firebase.ui.auth.viewmodel.smartlock;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.CredentialsUtil;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
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

    public SmartLockHandler(Application application) {
        super(application);
    }

    /**
     * Forward the result of a resolution from the Activity to the ViewModel.
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == RequestCodes.CRED_SAVE) {
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
    @RestrictTo(RestrictTo.Scope.TESTS)
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
            setResult(Resource.<Void>forFailure(new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR,
                    "Failed to build credential.")));
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
                            setResult(Resource.<Void>forFailure(
                                    new PendingIntentRequiredException(
                                            rae.getResolution(), RequestCodes.CRED_SAVE)));
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
