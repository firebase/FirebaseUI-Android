package com.firebase.ui.auth.ui.phone;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.data.model.PhoneNumberVerificationRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class CheckPhoneNumberHandler extends AuthViewModelBase<IdpResponse> {
    private static final long AUTO_RETRIEVAL_TIMEOUT_SECONDS = 120;

    private MutableLiveData<Resource<PhoneNumber>> mPhoneNumberListener = new MutableLiveData<>();
    private MutableLiveData<Exception> mVerificationErrorListener = new MutableLiveData<>();

    private String mPhoneNumber;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;

    public CheckPhoneNumberHandler(Application application) {
        super(application);
    }

    public LiveData<Resource<PhoneNumber>> getPhoneNumberListener() {
        return mPhoneNumberListener;
    }

    public void fetchCredential() {
        if (TextUtils.isEmpty(mPhoneNumber)) {
            setResult(Resource.<IdpResponse>forUsableFailure(new PendingIntentRequiredException(
                    Credentials.getClient(getApplication()).getHintPickerIntent(
                            new HintRequest.Builder().setPhoneNumberIdentifierSupported(true)
                                    .build()),
                    RequestCodes.CRED_HINT
            )));
        } else {
            mPhoneNumberListener.setValue(Resource.forSuccess(
                    PhoneNumberUtils.getPhoneNumber(mPhoneNumber)));
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) { return; }

        Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        String formattedPhone = PhoneNumberUtils.formatUsingCurrentCountry(
                credential.getId(), getApplication());
        if (formattedPhone != null) {
            mPhoneNumberListener.setValue(Resource.forSuccess(
                    PhoneNumberUtils.getPhoneNumber(formattedPhone)));
        }
    }

    public void verifyPhoneNumber(final String number, boolean force) {
        mPhoneNumber = number;
        getPhoneAuth().verifyPhoneNumber(
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
        // TODO
//        mSignInHandler.startSignIn(credential, new IdpResponse.Builder(
//                new User.Builder(PhoneAuthProvider.PROVIDER_ID, null)
//                        .setPhoneNumber(number)
//                        .build())
//                .build());
    }
}
