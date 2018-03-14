package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.util.AnonymousUpgradeUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;

/**
 * Handles the logic for {@link com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt} including
 * signing in with email and password, linking other credentials, and saving credentials to
 * SmartLock.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackPasswordHandler extends AuthViewModelBase {

    private static final String TAG = "WBPasswordHandler";

    private MutableLiveData<Resource<IdpResponse>> mSignInLiveData = new MutableLiveData<>();

    private String mPendingPassword;

    public WelcomeBackPasswordHandler(Application application) {
        super(application);
    }

    /**
     * Kick off the sign-in process.
     */
    public void startSignIn(@NonNull final String email,
                            @NonNull final String password,
                            @NonNull final IdpResponse inputResponse,
                            @Nullable final AuthCredential credential) {
        mSignInLiveData.setValue(Resource.<IdpResponse>forLoading());

        // Store the password before signing in so it can be used for later credential building
        mPendingPassword = password;

        // Build appropriate IDP response based on inputs
        final IdpResponse outputResponse;
        if (credential == null) {
            // New credential for the email provider
            outputResponse = new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, email).build())
                    .build();
        } else {
            // New credential for an IDP (Phone or Social)
            outputResponse = new IdpResponse.Builder(inputResponse.getUser())
                    .setToken(inputResponse.getIdpToken())
                    .setSecret(inputResponse.getIdpSecret())
                    .build();
        }

        // Before signing in, check to see if this is an anonymous upgrade and, if so, if we will
        // produce a conflict.
        checkAnonymousConflict(email, password)
                .continueWithTask(new Continuation<Void, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<Void> task) throws Exception {
                        // If validation failed, re-throw the exception
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Kick off the flow including signing in, linking accounts, and saving with SmartLock
                        return getAuth().signInWithEmailAndPassword(email, password);
                    }
                })
                .addOnFailureListener(new TaskFailureLogger(TAG, "signInWithEmailAndPassword failed."))
                .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<AuthResult> task) throws Exception {
                        // Forward task failure by asking for result
                        AuthResult result = task.getResult(Exception.class);

                        // Task succeeded, link user if necessary
                        if (credential == null) {
                            return Tasks.forResult(result);
                        } else {
                            return result.getUser()
                                    .linkWithCredential(credential)
                                    .continueWithTask(new ProfileMerger(outputResponse))
                                    .addOnFailureListener(new TaskFailureLogger(TAG, "linkWithCredential+merge failed."));
                        }
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            mSignInLiveData.setValue(Resource.<IdpResponse>forFailure(task.getException()));
                            return;
                        }

                       setSuccess(outputResponse);
                    }
                });
    }

    private Task<Void> checkAnonymousConflict(String email, String password) {
        if (!AnonymousUpgradeUtils.canUpgradeAnonymous(getArguments(), getAuth())) {
            return Tasks.forResult(null);
        }

        final AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        return AnonymousUpgradeUtils.validateCredential(getApp(), credential)
                .continueWith(new Continuation<Boolean, Void>() {
                    @Override
                    public Void then(@NonNull Task<Boolean> task) throws Exception {
                        if (task.isSuccessful() && task.getResult()) {
                            // Definitely conflict
                            throw new FirebaseUiException(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT);
                        } else if (!task.isSuccessful()) {
                            // Unknown error
                            throw new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR);
                        } else {
                            // No conflict
                            return null;
                        }
                    }
                });
    }

    /**
     * Get the observable state of the sign in operation.
     */
    public LiveData<Resource<IdpResponse>> getSignInOperation() {
        return mSignInLiveData;
    }

    /**
     * Get the most recent pending password.
     */
    public String getPendingPassword() {
        return mPendingPassword;
    }

    private void setSuccess(IdpResponse idpResponse) {
        mSignInLiveData.setValue(Resource.forSuccess(idpResponse));
    }
}
