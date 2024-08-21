package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;

import androidx.annotation.Nullable;

public class CheckEmailHandler extends AuthViewModelBase<User> {
    public CheckEmailHandler(Application application) {
        super(application);
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
        if (resultCode != Activity.RESULT_OK) { return; }

//        setResult(Resource.forLoading());
        // TODO(hackathon): Re-enable this flow
        setResult(Resource.forFailure(new IllegalStateException("Disabled for hackathon")));
//        final Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
//        final String email = credential.getId();
//        ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
//                .addOnCompleteListener(task -> {
//                    setResult(Resource.forFailure(task.getException()));
//                    // TODO(hackathon): re-enable this
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
