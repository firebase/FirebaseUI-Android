package com.firebase.ui.auth.viewmodel.smartlock;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.CredentialUtils;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * ViewModel for initiating saves to the Credentials API (SmartLock).
 */
public class SmartLockHandler extends AuthViewModelBase<IdpResponse> {
    private static final String TAG = "SmartLockViewModel";

    private IdpResponse mResponse;

    public SmartLockHandler(Application application) {
        super(application);
    }

    public void setResponse(@NonNull IdpResponse response) {
        mResponse = response;
    }

    /**
     * Forward the result of a resolution from the Activity to the ViewModel.
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == RequestCodes.CRED_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Resource.forSuccess(mResponse));
            } else {
                Log.e(TAG, "SAVE: Canceled by user.");
                FirebaseUiException exception = new FirebaseUiException(
                        ErrorCodes.UNKNOWN_ERROR, "Save canceled by user.");
                setResult(Resource.<IdpResponse>forFailure(exception));
            }
        }
    }

    /** @see #saveCredentials(Credential) */
    @RestrictTo(RestrictTo.Scope.TESTS)
    public void saveCredentials(FirebaseUser firebaseUser,
                                @Nullable String password,
                                @Nullable String accountType) {
        saveCredentials(CredentialUtils.buildCredential(firebaseUser, password, accountType));
    }

    /** Initialize saving a credential. */
    public void saveCredentials(@Nullable Credential credential) {
        if (!getArguments().enableCredentials) {
            setResult(Resource.forSuccess(mResponse));
            return;
        }
        setResult(Resource.<IdpResponse>forLoading());

        if (credential == null) {
            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException(
                    ErrorCodes.UNKNOWN_ERROR, "Failed to build credential.")));
            return;
        }

        deleteUnusedCredentials();
        getCredentialsClient().save(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.forSuccess(mResponse));
                        } else if (task.getException() instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) task.getException();
                            setResult(Resource.<IdpResponse>forFailure(
                                    new PendingIntentRequiredException(
                                            rae.getResolution(), RequestCodes.CRED_SAVE)));
                        } else {
                            Log.w(TAG, "Non-resolvable exception: " + task.getException());
                            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException(
                                    ErrorCodes.UNKNOWN_ERROR,
                                    "Error when saving credential.",
                                    task.getException())));
                        }
                    }
                });
    }

    private void deleteUnusedCredentials() {
        if (mResponse.getProviderType().equals(GoogleAuthProvider.PROVIDER_ID)) {
            // Since Google accounts upgrade email ones, we don't want to end up
            // with duplicate credentials so delete the email ones.
            String type = ProviderUtils.providerIdToAccountType(
                    GoogleAuthProvider.PROVIDER_ID);
            GoogleApiUtils.getCredentialsClient(getApplication()).delete(
                    CredentialUtils.buildCredentialOrThrow(getCurrentUser(), "pass", type));
        }
    }
}
