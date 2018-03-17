package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.SingleLiveEvent;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

@SuppressWarnings("WrongConstant")
public class CheckEmailHandler extends AuthViewModelBase<IdpResponse> {
    private MutableLiveData<User> mProviderListener = new SingleLiveEvent<>();

    public CheckEmailHandler(Application application) {
        super(application);
    }

    /**
     * This user can have a null provider in which case it's a new user.
     */
    public LiveData<User> getUserListener() {
        return mProviderListener;
    }

    public void fetchCredential() {
        setResult(Resource.<IdpResponse>forFailure(new PendingIntentRequiredException(
                Credentials.getClient(getApplication()).getHintPickerIntent(
                        new HintRequest.Builder().setEmailAddressIdentifierSupported(true).build()),
                RequestCodes.CRED_HINT
        )));
    }

    public void fetchProvider(final String email) {
        setResult(Resource.<IdpResponse>forLoading());
        ProviderUtils.fetchTopProvider(getAuth(), email)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        mProviderListener.setValue(task.isSuccessful() ?
                                new User.Builder(task.getResult(), email).build()
                                : null);
                    }
                });
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) { return; }

        setResult(Resource.<IdpResponse>forLoading());
        final Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        final String email = credential.getId();
        ProviderUtils.fetchTopProvider(getAuth(), email)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
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
