package com.firebase.ui.auth.ui.phone;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.data.model.PhoneNumberVerificationRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneNumberVerificationHandler extends AuthViewModelBase<PhoneVerification> {
    private static final long AUTO_RETRIEVAL_TIMEOUT_SECONDS = 120;
    private static final String VERIFICATION_ID_KEY = "verification_id";

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;

    public PhoneNumberVerificationHandler(Application application) {
        super(application);
    }

    public void verifyPhoneNumber(final String number, boolean force) {
        setResult(Resource.<PhoneVerification>forLoading());
        getPhoneAuth().verifyPhoneNumber(
                number,
                AUTO_RETRIEVAL_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        setResult(Resource.forSuccess(new PhoneVerification(
                                number, credential, true)));
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        setResult(Resource.<PhoneVerification>forFailure(e));
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        mForceResendingToken = token;
                        setResult(Resource.<PhoneVerification>forFailure(
                                new PhoneNumberVerificationRequiredException(number)));
                    }
                },
                force ? mForceResendingToken : null);
    }

    public void submitVerificationCode(String number, String code) {
        setResult(Resource.forSuccess(new PhoneVerification(
                number,
                PhoneAuthProvider.getCredential(mVerificationId, code),
                false)));
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(VERIFICATION_ID_KEY, mVerificationId);
    }

    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (mVerificationId == null && savedInstanceState != null) {
            mVerificationId = savedInstanceState.getString(VERIFICATION_ID_KEY);
        }
    }
}
