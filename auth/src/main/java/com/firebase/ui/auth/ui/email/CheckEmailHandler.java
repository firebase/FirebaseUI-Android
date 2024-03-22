package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.exceptions.GetCredentialException;

public class CheckEmailHandler extends AuthViewModelBase<User> {
    private CredentialManager mCredentialManager;
    private Context mContext;

    public CheckEmailHandler(Application application) {
        super(application);
        mContext = application.getApplicationContext();
        mCredentialManager = CredentialManager.create(mContext);
    }

    public void fetchProvider(final String email) {
        setResult(Resource.forLoading());
        ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        setResult(Resource.forSuccess(
                                new User.Builder(task.getResult(), email).build()));
                    } else {
                        setResult(Resource.forFailure(task.getException()));
                    }
                });
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // TODO(rosariopf): figure out where this was being called from
        //   and wether it's still neded
//        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) { return; }
//
//        setResult(Resource.forLoading());
//        final Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
//        final String email = credential.getId();
//        ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        setResult(Resource.forSuccess(new User.Builder(task.getResult(), email)
//                                .setName(credential.getName())
//                                .setPhotoUri(credential.getProfilePictureUri())
//                                .build()));
//                    } else {
//                        setResult(Resource.forFailure(task.getException()));
//                    }
//                });
    }
}
