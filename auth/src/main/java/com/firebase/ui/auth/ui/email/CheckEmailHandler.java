package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.firebase.ui.auth.data.model.ActivityResult;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.AuthViewModelBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.data.SingleLiveEvent;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

@SuppressWarnings("WrongConstant")
public class CheckEmailHandler extends AuthViewModelBase implements Observer<ActivityResult> {
    private static final int RC_HINT = 17;

    private MutableLiveData<User> mProviderListener = new SingleLiveEvent<>();
    private Pair<String, Task<String>> mCachedProviderFetch;

    public CheckEmailHandler(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowHolder args) {
        super.onCreate(args);
        mFlowHolder.getActivityResultListener().observeForever(this);
    }

    /**
     * This user can have a null provider in which case it's a new user.
     */
    public LiveData<User> getProviderListener() {
        return mProviderListener;
    }

    public void fetchCredential() {
        mFlowHolder.getProgressListener().setValue(false);
        mFlowHolder.getPendingIntentStarter().setValue(Pair.create(getEmailHintIntent(), RC_HINT));
    }

    public void fetchProvider(final String email) {
        mFlowHolder.getProgressListener().setValue(false);
        updateFetchProviderCache(email);
        mCachedProviderFetch.second.addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                mFlowHolder.getProgressListener().setValue(true);
                mProviderListener.setValue(task.isSuccessful() ?
                        new User.Builder(task.getResult(), email).build()
                        : null);
            }
        });
    }

    private void updateFetchProviderCache(String email) {
        if (mCachedProviderFetch == null || !TextUtils.equals(email, mCachedProviderFetch.first)) {
            mCachedProviderFetch = Pair.create(email, ProviderUtils.fetchTopProvider(mAuth, email));
        }
    }

    private PendingIntent getEmailHintIntent() {
        return Auth.CredentialsApi.getHintPickerIntent(
                new GoogleApiClient.Builder(getApplication()).addApi(Auth.CREDENTIALS_API).build(),
                new HintRequest.Builder().setEmailAddressIdentifierSupported(true).build());
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        if (result.getRequestCode() == RC_HINT) {
            if (result.getResultCode() != Activity.RESULT_OK) {
                mFlowHolder.getProgressListener().setValue(true);
                return;
            }

            final Credential credential = result.getData().getParcelableExtra(Credential.EXTRA_KEY);

            final String email = credential.getId();
            updateFetchProviderCache(email);
            mCachedProviderFetch.second.addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    mFlowHolder.getProgressListener().setValue(true);
                    mProviderListener.setValue(task.isSuccessful() ?
                            new User.Builder(task.getResult(), email)
                                    .setName(credential.getName())
                                    .setPhotoUri(credential.getProfilePictureUri())
                                    .build()
                            : null);
                }
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
    }
}
