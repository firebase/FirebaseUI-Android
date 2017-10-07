package com.firebase.ui.auth.data.remote;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CyclicAccountLinkingException;
import com.firebase.ui.auth.data.model.UnknownErrorException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.util.data.ProfileMerger;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.data.SingleLiveEvent;
import com.firebase.ui.auth.util.ui.ActivityResult;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.firebase.ui.auth.util.ui.ViewModelBase;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

public class SignInHandler extends ViewModelBase<FlowHolder> {
    private static final int RC_ACCOUNT_LINK = 3;

    private static final MutableLiveData<IdpResponse> SUCCESS_LISTENER = new SingleLiveEvent<>();
    private static final MutableLiveData<FirebaseAuthException> FAILURE_LISTENER = new SingleLiveEvent<>();

    private FlowHolder mFlowHolder;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;

    public SignInHandler(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowHolder args) {
        mFlowHolder = args;

        FirebaseApp app = FirebaseApp.getInstance(mFlowHolder.getParams().appName);
        mAuth = FirebaseAuth.getInstance(app);
        mPhoneAuth = PhoneAuthProvider.getInstance(mAuth);
    }

    public LiveData<IdpResponse> getSuccessLiveData() {
        return SUCCESS_LISTENER;
    }

    public LiveData<FirebaseAuthException> getFailureLiveData() {
        return FAILURE_LISTENER;
    }

    public void start(Task<IdpResponse> tokenTask) {
        tokenTask.addOnCompleteListener(new IdpResponseFlow());
    }

    private class IdpResponseFlow implements OnCompleteListener<IdpResponse> {
        @Override
        public void onComplete(@NonNull Task<IdpResponse> task) {
            if (task.isSuccessful()) {
                IdpResponse response = task.getResult();

                Task<AuthResult> base;
                switch (response.getProviderType()) {
                    case EmailAuthProvider.PROVIDER_ID:
                        base = handleEmail(response);
                        break;
                    case PhoneAuthProvider.PROVIDER_ID:
                        base = handlePhone(response);
                        break;
                    default:
                        base = handleIdp(response);
                }
                base.continueWithTask(new ProfileMerger(response))
                        .addOnSuccessListener(new SaveCredentialFlow(response))
                        .addOnFailureListener(new FailureFlow(response));
            } else {
                onFailure(task.getException());
            }
        }

        private Task<AuthResult> handleEmail(IdpResponse response) {
            return mAuth.createUserWithEmailAndPassword(
                    response.getEmail(), response.getPassword());
        }

        private Task<AuthResult> handlePhone(IdpResponse response) {
            // TODO
            throw new IllegalStateException("TODO");
        }

        private Task<AuthResult> handleIdp(IdpResponse response) {
            AuthCredential credential = ProviderUtils.getAuthCredential(response);
            Task<AuthResult> base;

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                base = mAuth.signInWithCredential(credential);
            } else {
                base = currentUser.linkWithCredential(credential);
            }

            return base;
        }
    }

    private class SaveCredentialFlow implements OnSuccessListener<AuthResult>,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
            ResultCallback<Status>, Observer<ActivityResult> {
        private static final int RC_SAVE = 100;
        private static final int RC_CONNECTION = 28;

        private final IdpResponse mResponse;

        private String mEmail;
        private String mPassword;
        private String mName;
        private String mProfilePictureUri;

        private GoogleApiClient mClient;

        public SaveCredentialFlow(IdpResponse response) {
            mResponse = response;

            mFlowHolder.getOnActivityResult().observeForever(this);
        }

        @Override
        public void onSuccess(AuthResult result) {
            if (!mFlowHolder.getParams().enableCredentials) {
                finish();
                return;
            }

            FirebaseUser user = result.getUser();
            mEmail = user.getEmail();
            mPassword = mResponse.getPassword();
            mName = user.getDisplayName();
            mProfilePictureUri = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

            mClient = new GoogleApiClient.Builder(getApplication())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Auth.CREDENTIALS_API)
                    .build();
            mClient.connect();
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Credential.Builder builder = new Credential.Builder(mEmail);
            builder.setPassword(mPassword);
            if (mPassword == null && mResponse != null) {
                builder.setAccountType(ProviderUtils.providerIdToAccountType(
                        mResponse.getProviderType()));
            }

            if (mName != null) {
                builder.setName(mName);
            }

            if (mProfilePictureUri != null) {
                builder.setProfilePictureUri(Uri.parse(mProfilePictureUri));
            }

            Auth.CredentialsApi.save(mClient, builder.build()).setResultCallback(this);
        }

        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                finish();
            } else {
                if (status.hasResolution()) {
                    // This will prompt the user if the credential is new.
                    mFlowHolder.getPendingIntentStarter()
                            .setValue(Pair.create(status.getResolution(), RC_SAVE));
                } else {
                    finish();
                }
            }
        }

        private void finish() {
            mClient.disconnect();
            mFlowHolder.getOnActivityResult().removeObserver(this);
            SUCCESS_LISTENER.setValue(mResponse);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            if (result.hasResolution()) {
                mFlowHolder.getPendingIntentStarter()
                        .setValue(Pair.create(result.getResolution(), RC_CONNECTION));
            }
        }

        @Override
        public void onChanged(@Nullable ActivityResult result) {
            if (result.getRequestCode() == RC_SAVE) {
                finish();
            } else if (result.getRequestCode() == RC_CONNECTION) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    mClient.connect();
                } else {
                    finish();
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            // Just wait
        }
    }

    private class FailureFlow implements OnFailureListener, OnSuccessListener<String>,
            Observer<ActivityResult> {
        private final IdpResponse mResponse;

        public FailureFlow(IdpResponse response) {
            mResponse = response;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            if (e instanceof FirebaseAuthUserCollisionException) {
                String email = mResponse.getEmail();
                if (email != null) {
                    ProviderUtils.fetchTopProvider(mAuth, email)
                            .addOnSuccessListener(this)
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    SignInHandler.this.onFailure(e);
                                }
                            });
                    return;
                }
            }

            SignInHandler.this.onFailure(e);
        }

        @Override
        public void onSuccess(String provider) {
            try {
                mFlowHolder.getOnActivityResult().observeForever(this);
            } catch (IllegalStateException e) {
                FAILURE_LISTENER.setValue(new CyclicAccountLinkingException(
                        "Attempting 3 way linking"));
                return;
            }

            Toast.makeText(getApplication(), R.string.fui_error_user_collision, Toast.LENGTH_LONG)
                    .show();

            if (provider == null) {
                throw new IllegalStateException(
                        "No provider even though we received a FirebaseAuthUserCollisionException");
            } else if (provider.equals(EmailAuthProvider.PROVIDER_ID)) {
                // Start email welcome back flow
                mFlowHolder.getIntentStarter().setValue(Pair.create(
                        WelcomeBackPasswordPrompt.createIntent(
                                getApplication(),
                                mFlowHolder.getParams(),
                                mResponse),
                        RC_ACCOUNT_LINK));
            } else {
                // Start Idp welcome back flow
                mFlowHolder.getIntentStarter().setValue(Pair.create(
                        WelcomeBackIdpPrompt.createIntent(
                                getApplication(),
                                mFlowHolder.getParams(),
                                new User.Builder(provider, mResponse.getEmail()).build(),
                                mResponse),
                        RC_ACCOUNT_LINK));
            }
        }

        @Override
        public void onChanged(@Nullable ActivityResult result) {
            if (result.getRequestCode() != RC_ACCOUNT_LINK) { return; }

            IdpResponse response = IdpResponse.fromResultIntent(result.getData());
            if (result.getResultCode() == Activity.RESULT_OK) {
                onExistingCredentialRetrieved();
            } else {
                onExistingCredentialRetrievalFailure(response);
            }

            mFlowHolder.getOnActivityResult().removeObserver(this);
        }

        private void onExistingCredentialRetrieved() {
            mAuth.getCurrentUser()
                    .linkWithCredential(ProviderUtils.getAuthCredential(mResponse))
                    .continueWithTask(new ProfileMerger(mResponse))
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // If linking the user's existing account with the new one fails,
                            // we just give up because the user's already signed-in anyway.
                            SUCCESS_LISTENER.setValue(mResponse);
                        }
                    });
        }

        private void onExistingCredentialRetrievalFailure(IdpResponse existingUserResponse) {
            FirebaseAuthException e = existingUserResponse.getException();
            if (e instanceof CyclicAccountLinkingException) {
                // 3 way linking: this will happen when:
                // 1. The user is already logged-in before starting the auth flow
                // 2. The user already another account that will cause linking exceptions
                // 3. The user tries to log in with a third account
                // For example, the user is logged-in anonymously, has a Google account, and tries
                // to log in with Facebook.

                onFailure(e);
                // For now, just fail. In the future, we can support a data migration where the
                // anonymous account is thrown away and the other two are linked
            } else {
                onFailure(e);
            }
        }
    }

    private void onFailure(@NonNull Exception e) {
        if (e instanceof FirebaseAuthException) {
            FAILURE_LISTENER.setValue((FirebaseAuthException) e);
        } else {
            FAILURE_LISTENER.setValue(new UnknownErrorException(String.valueOf(e.getMessage())));
        }
    }
}
