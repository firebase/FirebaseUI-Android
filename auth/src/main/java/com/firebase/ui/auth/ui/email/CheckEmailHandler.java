package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.util.data.AuthViewModel;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.ActivityResult;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;

public class CheckEmailHandler extends AuthViewModel implements Observer<ActivityResult> {
    private static final int RC_HINT = 17;

    private MutableLiveData<Credential> mCredentialListener = new MutableLiveData<>();
    private Pair<String, Task<String>> mCachedProviderFetch;

    public CheckEmailHandler(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowHolder args) {
        super.onCreate(args);
        mFlowHolder.getActivityResultListener().observeForever(this);
    }

    public LiveData<Credential> fetchCredential() {
        mFlowHolder.getPendingIntentStarter().setValue(Pair.create(getEmailHintIntent(), RC_HINT));
        return getCredentialListener();
    }

    public LiveData<Credential> getCredentialListener() {
        return mCredentialListener;
    }

    public Task<String> getTopProvider(String email) {
        if (mCachedProviderFetch == null || !TextUtils.equals(email, mCachedProviderFetch.first)) {
            mCachedProviderFetch = Pair.create(email, ProviderUtils.fetchTopProvider(mAuth, email));
        }

        return mCachedProviderFetch.second;
    }

    private PendingIntent getEmailHintIntent() {
        return Auth.CredentialsApi.getHintPickerIntent(
                new GoogleApiClient.Builder(getApplication())
                        .addApi(Auth.CREDENTIALS_API)
                        .build(),
                new HintRequest.Builder()
                        .setHintPickerConfig(new CredentialPickerConfig.Builder()
                                .setShowCancelButton(true)
                                .build())
                        .setEmailAddressIdentifierSupported(true)
                        .build());
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        if (result.getRequestCode() != RC_HINT) { return; }

        mCredentialListener.setValue(result.getResultCode() == Activity.RESULT_OK ?
                (Credential) result.getData().getParcelableExtra(Credential.EXTRA_KEY)
                : null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
    }
}
