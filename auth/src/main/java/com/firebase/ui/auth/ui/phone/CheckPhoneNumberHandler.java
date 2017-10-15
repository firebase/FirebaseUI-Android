package com.firebase.ui.auth.ui.phone;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.ActivityResult;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.data.model.PhoneNumberVerificationRequiredException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.SignInHandler;
import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.data.SingleLiveEvent;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class CheckPhoneNumberHandler extends AuthViewModelBase implements Observer<ActivityResult> {
    private static final long AUTO_RETRIEVAL_TIMEOUT_SECONDS = 120;
    private static final int RC_HINT = 14;

    private MutableLiveData<PhoneNumber> mPhoneNumberListener = new SingleLiveEvent<>();
    private MutableLiveData<Exception> mVerificationErrorListener = new SingleLiveEvent<>();
    private SignInHandler mHandler;

    private String mPhoneNumber;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;

    public CheckPhoneNumberHandler(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowHolder args) {
        super.onCreate(args);
        mFlowHolder.getActivityResultListener().observeForever(this);
    }

    public void setSignInHandler(SignInHandler handler) {
        mHandler = handler;
    }

    public LiveData<PhoneNumber> getPhoneNumberListener() {
        return mPhoneNumberListener;
    }

    public LiveData<Exception> getVerificationErrorListener() {
        return mVerificationErrorListener;
    }

    public void fetchCredential() {
        if (mPhoneNumber == null) {
            mFlowHolder.getPendingIntentStarter()
                    .setValue(Pair.create(getPhoneNumberHintIntent(), RC_HINT));
        }
    }

    private PendingIntent getPhoneNumberHintIntent() {
        return Auth.CredentialsApi.getHintPickerIntent(
                new GoogleApiClient.Builder(getApplication()).addApi(Auth.CREDENTIALS_API).build(),
                new HintRequest.Builder().setPhoneNumberIdentifierSupported(true).build());
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        if (result.getRequestCode() == RC_HINT && result.getResultCode() == Activity.RESULT_OK) {
            final Credential credential = result.getData().getParcelableExtra(Credential.EXTRA_KEY);

            String formattedPhone = PhoneNumberUtils.formatUsingCurrentCountry(
                    credential.getId(), getApplication());
            if (formattedPhone != null) {
                mPhoneNumberListener.setValue(PhoneNumberUtils.getPhoneNumber(formattedPhone));
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
    }

    public void verifyPhoneNumber(final String number, boolean force) {
        mPhoneNumber = number;
        mPhoneAuth.verifyPhoneNumber(
                number,
                AUTO_RETRIEVAL_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        start(credential, number);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        mVerificationErrorListener.setValue(e);
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        mForceResendingToken = token;
                        mVerificationErrorListener.setValue(
                                new PhoneNumberVerificationRequiredException(number));
                    }
                },
                force ? mForceResendingToken : null);
    }

    public void submitVerificationCode(String code) {
        start(PhoneAuthProvider.getCredential(mVerificationId, code), mPhoneNumber);
    }

    private void start(PhoneAuthCredential credential, String number) {
        mHandler.signIn(
                new IdpResponse.Builder(
                        new User.Builder(PhoneAuthProvider.PROVIDER_ID, null)
                                .setPhoneNumber(number)
                                .build())
                        .build(),
                credential);
    }
}
