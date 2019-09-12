package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;
import android.text.TextUtils;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.EmailLinkParser;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager.SessionRecord;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
            setResult(Resource.<IdpResponse>forFailure(
                    new FirebaseUiException(ErrorCodes.INVALID_EMAIL_LINK_ERROR)));
            return;
        }

        final EmailLinkPersistenceManager persistenceManager = EmailLinkPersistenceManager
                .getInstance();

        SessionRecord sessionRecord = persistenceManager.retrieveSessionRecord(getApplication());

        EmailLinkParser parser = new EmailLinkParser(link);
        String sessionIdFromLink = parser.getSessionId();
        String anonymousUserIdFromLink = parser.getAnonymousUserId();
        String oobCodeFromLink = parser.getOobCode();
        String providerIdFromLink = parser.getProviderId();
        boolean forceSameDevice = parser.getForceSameDeviceBit();

        if (isDifferentDeviceFlow(sessionRecord, sessionIdFromLink)) {
            if (TextUtils.isEmpty(sessionIdFromLink)) {
                // There should always be a valid session ID in the link
                setResult(Resource.<IdpResponse>forFailure(
                        new FirebaseUiException(ErrorCodes.INVALID_EMAIL_LINK_ERROR)));
                return;
            }
            if (forceSameDevice || !TextUtils.isEmpty(anonymousUserIdFromLink)) {
                // In both cases, the link was meant to be completed on the same device.
                // For anonymous user upgrade, we don't support the cross device flow.
                setResult(Resource.<IdpResponse>forFailure(
                        new FirebaseUiException(ErrorCodes.EMAIL_LINK_WRONG_DEVICE_ERROR)));
                return;
            }

            // If we have no SessionRecord/there is a session ID mismatch, this means that we were
            // not the ones to send the link. The only way forward is to prompt the user for their
            // email before continuing the flow. We should only do that after validating the link.
            determineDifferentDeviceErrorFlowAndFinish(oobCodeFromLink, providerIdFromLink);
            return;
        }

        if (anonymousUserIdFromLink != null){
            // Same device flow, need to ensure uids match
            if (getAuth().getCurrentUser() == null
                    || (getAuth().getCurrentUser().isAnonymous()
                    && !anonymousUserIdFromLink.equals(getAuth().getCurrentUser().getUid()))) {
                setResult(Resource.<IdpResponse>forFailure(
                        new FirebaseUiException(
                                ErrorCodes.EMAIL_LINK_DIFFERENT_ANONYMOUS_USER_ERROR)));
                return;
            }
        }

        finishSignIn(sessionRecord);
    }

    public void finishSignIn(String email) {
        setResult(Resource.<IdpResponse>forLoading());
        finishSignIn(email, /*response=*/null);
    }

    private void finishSignIn(SessionRecord sessionRecord) {
        String email = sessionRecord.getEmail();
        IdpResponse response = sessionRecord.getIdpResponseForLinking();
        finishSignIn(email, response);
    }

    private void finishSignIn(@NonNull String email, @Nullable IdpResponse response) {
        if (TextUtils.isEmpty(email)) {
            setResult(Resource.<IdpResponse>forFailure(
                    new FirebaseUiException(ErrorCodes.EMAIL_MISMATCH_ERROR)));
            return;
        }
        final AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        final EmailLinkPersistenceManager persistenceManager = EmailLinkPersistenceManager
                .getInstance();
        String link = getArguments().emailLink;
        if (response == null) {
            handleNormalFlow(authOperationManager, persistenceManager, email, link);
        } else {
            handleLinkingFlow(authOperationManager, persistenceManager, response, link);
        }
    }

    private void determineDifferentDeviceErrorFlowAndFinish(@NonNull String oobCode,
                                                            @Nullable final String providerId) {
        getAuth().checkActionCode(oobCode).addOnCompleteListener(
                new OnCompleteListener<ActionCodeResult>() {
                    @Override
                    public void onComplete(@NonNull Task<ActionCodeResult> task) {
                        if (task.isSuccessful()) {
                            if (!TextUtils.isEmpty(providerId)) {
                                setResult(Resource.<IdpResponse>forFailure(
                                        new FirebaseUiException(
                                                ErrorCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_ERROR)));
                                return;
                            }
                            // The email link is valid, we can ask the user for their email
                            setResult(Resource.<IdpResponse>forFailure(
                                            new FirebaseUiException(
                                                    ErrorCodes.EMAIL_LINK_PROMPT_FOR_EMAIL_ERROR)));
                        } else {
                            setResult(Resource.<IdpResponse>forFailure(
                                    new FirebaseUiException(ErrorCodes.INVALID_EMAIL_LINK_ERROR)));
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
                                setResult(Resource.<IdpResponse>forFailure(task.getException()));
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

    private boolean isDifferentDeviceFlow(SessionRecord sessionRecord, String sessionIdFromLink) {
        return sessionRecord == null || TextUtils.isEmpty(sessionRecord.getSessionId())
                || TextUtils.isEmpty(sessionIdFromLink)
                || !sessionIdFromLink.equals(sessionRecord.getSessionId());
    }
}
