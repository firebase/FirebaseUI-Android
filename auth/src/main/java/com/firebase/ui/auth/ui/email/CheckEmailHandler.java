package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class CheckEmailHandler extends AuthViewModelBase<User> {
    public CheckEmailHandler(Application application) {
        super(application);
    }

    public void fetchCredential() {
        setResult(Resource.<User>forFailure(new PendingIntentRequiredException(
                Credentials.getClient(getApplication()).getHintPickerIntent(
                        new HintRequest.Builder().setEmailAddressIdentifierSupported(true).build()),
                RequestCodes.CRED_HINT
        )));
    }

    public void fetchProvider(final String email) {
        setResult(Resource.<User>forLoading());
        ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.forSuccess(
                                    new User.Builder(task.getResult(), email).build()));
                        } else {
                            setResult(Resource.<User>forFailure(task.getException()));
                        }
                    }
                });
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) { return; }

        setResult(Resource.<User>forLoading());
        final Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        final String email = credential.getId();
        ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.forSuccess(new User.Builder(task.getResult(), email)
                                    .setName(credential.getName())
                                    .setPhotoUri(credential.getProfilePictureUri())
                                    .build()));
                        } else {
                            setResult(Resource.<User>forFailure(task.getException()));
                        }
                    }
                });
    }
}
