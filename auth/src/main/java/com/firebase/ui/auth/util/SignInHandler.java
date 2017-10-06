package com.firebase.ui.auth.util;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.provider.ProviderUtils;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.ui.accountlink.WelcomeBackPasswordPrompt;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

public class SignInHandler extends ViewModelBase<FlowHolder> {
    private static final String TAG = "SignInHandler";
    private static final int RC_ACCOUNT_LINK = 3;

    private FlowHolder mFlowHolder;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider mPhoneAuth;

    private final MutableLiveData<IdpResponse> mSuccessListener = new SingleLiveEvent<>();
    private final MutableLiveData<Exception> mFailureListener = new SingleLiveEvent<>();

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
        return mSuccessListener;
    }

    public LiveData<Exception> getFailureLiveData() {
        return mFailureListener;
    }

    public void start(Task<IdpResponse> tokenTask) {
        tokenTask.addOnCompleteListener(new IdpResponseFlow());
    }

    private class IdpResponseFlow implements OnCompleteListener<IdpResponse> {
        @Override
        public void onComplete(@NonNull Task<IdpResponse> task) {
            if (task.isSuccessful()) {
                IdpResponse response = task.getResult();
                mAuth.signInWithCredential(ProviderUtils.getAuthCredential(response))
                        .addOnSuccessListener(new SuccessFlow(response))
                        .addOnFailureListener(new FailureFlow(response));
            } else {
                mFailureListener.setValue(task.getException());
            }
        }
    }

    private class SuccessFlow implements OnSuccessListener<AuthResult>,
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

        public SuccessFlow(IdpResponse response) {
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
            mPassword = password;
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
                    Log.w(TAG, "Status message:\n" + status.getStatusMessage());
                    finish();
                }
            }
        }

        private void finish() {
            mClient.disconnect();
            mFlowHolder.getOnActivityResult().removeObserver(this);
            mSuccessListener.setValue(mResponse);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            Log.w(TAG, "onConnectionFailed:" + result);
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

    private class FailureFlow implements OnFailureListener, OnSuccessListener<String> {
        private final IdpResponse mResponse;

        public FailureFlow(IdpResponse response) {
            mResponse = response;

            mFlowHolder.getOnActivityResult().observeForever(new Observer<ActivityResult>() {
                @Override
                public void onChanged(@Nullable ActivityResult result) {
                    if (result.getRequestCode() == RC_ACCOUNT_LINK) {
                        IdpResponse idpResponse = IdpResponse.fromResultIntent(result.getData());
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            mSuccessListener.setValue(idpResponse);
                        } else {
                            mFailureListener.setValue(new SignInFailedException(
                                    String.valueOf(idpResponse.getErrorCode()),
                                    "Account linking failed"));
                        }

                        mFlowHolder.getOnActivityResult().removeObserver(this);
                    }
                }
            });
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
                                    mFailureListener.setValue(e);
                                }
                            });
                    return;
                }
            } else {
                Log.e(TAG,
                        "Unexpected exception when signing in with credential "
                                + mResponse.getProviderType()
                                + " unsuccessful. Visit https://console.firebase.google.com to enable it.",
                        e);
            }

            mFailureListener.setValue(e);
        }

        @Override
        public void onSuccess(String provider) {
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
    }
}
