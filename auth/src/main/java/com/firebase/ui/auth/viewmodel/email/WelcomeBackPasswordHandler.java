package com.firebase.ui.auth.viewmodel.email;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.firebase.ui.auth.viewmodel.PendingFinish;
import com.firebase.ui.auth.viewmodel.PendingResolution;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;

/**
 * TODO(samstern): Document
 *
 * TODO(samstern): Use TaskFailureLogger everywhere
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackPasswordHandler extends AuthViewModelBase {

    private static final String TAG = "WBPasswordHandler";

    private static final int RC_SAVE = 100;

    // TODO: Should this be a SingleLiveEvent?
    // https://github.com/googlesamples/android-architecture/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java
    private MutableLiveData<PendingResolution> mPendingResolutionLiveData = new MutableLiveData<>();
    private MutableLiveData<PendingFinish> mPendingFinishLiveData = new MutableLiveData<>();
    private MutableLiveData<Resource<AuthResult>> mSignInLiveData = new MutableLiveData<>();

    public WelcomeBackPasswordHandler(Application application) {
        super(application);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SAVE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "SAVE: Canceled by user");
            }

            // TODO: finish
            mPendingResolutionLiveData.setValue(null);
            return true;
        }

        return false;
    }

    public void startSignIn(@NonNull final String email,
                            @NonNull final String password,
                            @NonNull final IdpResponse inputResponse,
                            @Nullable final AuthCredential credential) {
        mSignInLiveData.setValue(new Resource<AuthResult>());

        // Build appropriate IDP response based on inputs
        final IdpResponse outputResponse;
        if (credential== null) {
            outputResponse = new IdpResponse.Builder(
                    new User.Builder(EmailAuthProvider.PROVIDER_ID, email).build())
                    .build();
        } else {
            outputResponse = new IdpResponse.Builder(inputResponse.getUser())
                    .setToken(inputResponse.getIdpToken())
                    .setSecret(inputResponse.getIdpSecret())
                    .build();
        }

        // Kick off the flow including signing in, linking accounts, and saving with SmartLock
        getAuth().signInWithEmailAndPassword(email, password)
                .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<AuthResult> task) throws Exception {
                        // Forward task failure
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Task succeeded, link user if necessary
                        if (credential == null) {
                            return Tasks.forResult(task.getResult());
                        } else {
                            return task.getResult().getUser()
                                    .linkWithCredential(credential)
                                    .continueWithTask(new ProfileMerger(outputResponse));
                        }
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            mSignInLiveData.setValue(new Resource<AuthResult>(task.getException()));
                            return;
                        }

                        AuthResult authResult = task.getResult();
                        saveCredentialsOrFinish(authResult.getUser(), password, outputResponse);
                    }
                });
    }

    public LiveData<Resource<AuthResult>> getSignInResult() {
        return mSignInLiveData;
    }

    public LiveData<PendingFinish> getPendingFinish() {
        return mPendingFinishLiveData;
    }

    public LiveData<PendingResolution> getPendingResolution() {
        return mPendingResolutionLiveData;
    }

    private void finish(IdpResponse idpResponse) {
        PendingFinish pendingFinish = new PendingFinish(Activity.RESULT_OK, idpResponse.toIntent());
        mPendingFinishLiveData.setValue(pendingFinish);
    }

    private void saveCredentialsOrFinish(FirebaseUser user,
                                         @Nullable String password,
                                         final IdpResponse idpResponse) {

        if (!getArguments().enableCredentials) {
            finish(idpResponse);
            return;
        }

        Credential credential = SaveSmartLock.buildCredential(user, password, idpResponse);
        if (credential == null) {
            finish(idpResponse);
            return;
        }

        getCredentialsClient().save(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            finish(idpResponse);
                            return;
                        }

                        if (task.getException() instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) task.getException();
                            PendingResolution pendingResolution = new PendingResolution(rae.getResolution(), RC_SAVE);
                            mPendingResolutionLiveData.setValue(pendingResolution);
                        } else {
                            finish(idpResponse);
                        }
                    }
                });
    }
}
