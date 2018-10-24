package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.EmailLinkParser;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.data.TaskFailureLogger;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeResult;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.AuthUI.EMAIL_LINK_PROVIDER;

public class EmailLinkSignInHandler extends SignInViewModelBase {
    private static final String TAG = "EmailLinkSignInHandler";

    public EmailLinkSignInHandler(Application application) {
        super(application);
    }

    public void startSignIn() {
        setResult(Resource.<IdpResponse>forLoading());

        String link = getArguments().emailLink;
        if (!getAuth().isSignInWithEmailLink(link)) {
            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException(ErrorCodes
                    .INVALID_EMAIL_LINK_ERROR)));
            return;
        }

        final EmailLinkPersistenceManager persistenceManager = EmailLinkPersistenceManager
                .getInstance();
        final AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        final IdpResponse response = persistenceManager.retrieveIdpResponseForLinking
                (getApplication());
        final String email = persistenceManager.retrieveEmailForLink(getApplication());

        persistenceManager.clearAllData(getApplication());

        if (email == null && response == null) {
            determineErrorFlowAndFinish(link);
        } else if (response != null) {
            handleLinkingFlow(authOperationManager, persistenceManager, response, link);
        } else {
            handleNormalFlow(authOperationManager, persistenceManager, email, link);
        }
    }

    private void determineErrorFlowAndFinish(String link) {
        String oobCode = EmailLinkParser.getInstance().getOobCodeFromLink(link);
        getAuth().checkActionCode(oobCode).addOnCompleteListener(
                new OnCompleteListener<ActionCodeResult>() {
                    @Override
                    public void onComplete(@NonNull Task<ActionCodeResult> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException
                                    (ErrorCodes
                                            .EMAIL_LINK_WRONG_DEVICE_ERROR)));
                        } else {
                            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException
                                    (ErrorCodes
                                            .INVALID_EMAIL_LINK_ERROR)));
                        }
                    }
                });
    }

    private void handleLinkingFlow(final AuthOperationManager authOperationManager,
                                   final EmailLinkPersistenceManager persistenceManager,
                                   final IdpResponse response,
                                   final String link) {
        final AuthCredential storedCredentialForLink = ProviderUtils.getAuthCredential
                (response);
        final AuthCredential emailLinkCredential = EmailAuthProvider.getCredentialWithLink
                (response.getEmail(), link);

        if (authOperationManager.canUpgradeAnonymous(getAuth(), getArguments())) {
            authOperationManager.safeLink(emailLinkCredential,
                    storedCredentialForLink, getArguments())
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            persistenceManager.clearAllData(getApplication());
                            if (task.isSuccessful()) {
                                handleMergeFailure(storedCredentialForLink);
                            } else {
                                setResult(Resource.<IdpResponse>forFailure(task.getException
                                        ()));
                            }
                        }
                    });
        } else {
            getAuth().signInWithCredential(emailLinkCredential)
                    .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
                            persistenceManager.clearAllData(getApplication());
                            if (!task.isSuccessful()) {
                                return task;
                            }
                            return task.getResult().getUser()
                                    .linkWithCredential(storedCredentialForLink)
                                    .continueWithTask(new ProfileMerger(response))
                                    .addOnFailureListener(new TaskFailureLogger(TAG,
                                            "linkWithCredential+merge failed."));
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            FirebaseUser user = authResult.getUser();
                            IdpResponse response = new IdpResponse.Builder(
                                    new User.Builder(EmailAuthProvider
                                            .EMAIL_LINK_SIGN_IN_METHOD,
                                            user.getEmail())
                                            .setName(user.getDisplayName())
                                            .setPhotoUri(user.getPhotoUrl())
                                            .build())
                                    .build();
                            handleSuccess(response, authResult);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            setResult(Resource.<IdpResponse>forFailure(e));
                        }
                    });
        }
    }

    private void handleNormalFlow(final AuthOperationManager authOperationManager,
                                  final EmailLinkPersistenceManager persistenceManager,
                                  final String email,
                                  final String link) {
        final AuthCredential emailLinkCredential = EmailAuthProvider.getCredentialWithLink(email,
                link);

        // Bug in core SDK - credential is mutated and won't be usable for sign in, so create
        // a new one to pass back. b/117425827
        final AuthCredential emailLinkCredentialForLinking = EmailAuthProvider
                .getCredentialWithLink(email,
                        link);
        // Either regular sign in or anonymous user upgrade
        authOperationManager.signInAndLinkWithCredential(getAuth(), getArguments(),
                emailLinkCredential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        persistenceManager.clearAllData(getApplication());

                        FirebaseUser user = authResult.getUser();
                        IdpResponse response = new IdpResponse.Builder(
                                new User.Builder(EMAIL_LINK_PROVIDER,
                                        user.getEmail())
                                        .setName(user.getDisplayName())
                                        .setPhotoUri(user.getPhotoUrl())
                                        .build())
                                .build();
                        handleSuccess(response, authResult);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        persistenceManager.clearAllData(getApplication());
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            handleMergeFailure(emailLinkCredentialForLinking);
                        } else {
                            setResult(Resource.<IdpResponse>forFailure(e));
                        }
                    }
                });
    }
}
