package com.firebase.ui.auth.ui.email;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// New Identity API imports:
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.identity.Identity;

import androidx.annotation.Nullable;

public class CheckEmailHandler extends AuthViewModelBase<User> {
    private static final String TAG = "CheckEmailHandler";

    public CheckEmailHandler(Application application) {
        super(application);
    }

    /**
     * Initiates a hint picker flow using the new Identity API.
     * This replaces the deprecated Credentials API call.
     */
    public void fetchCredential() {
        // Build a sign-in request that supports password-based sign in,
        // which will trigger the hint picker UI for email addresses.
        SignInClient signInClient = Identity.getSignInClient(getApplication());
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setPasswordRequestOptions(
                        BeginSignInRequest.PasswordRequestOptions.builder()
                                .setSupported(true)
                                .build())
                .build();

        signInClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    // The new API returns a PendingIntent to launch the hint picker.
                    PendingIntent pendingIntent = result.getPendingIntent();
                    setResult(Resource.forFailure(
                            new PendingIntentRequiredException(pendingIntent, RequestCodes.CRED_HINT)));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "beginSignIn failed", e);
                    setResult(Resource.forFailure(e));
                });
    }

    /**
     * Fetches the top provider for the given email.
     */
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

    /**
     * Handles the result from the hint picker launched via the new Identity API.
     */
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) {
            return;
        }

        setResult(Resource.forLoading());
        SignInClient signInClient = Identity.getSignInClient(getApplication());
        try {
            // Retrieve the SignInCredential from the returned intent.
            SignInCredential credential = signInClient.getSignInCredentialFromIntent(data);
            final String email = credential.getId();

            ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            setResult(Resource.forSuccess(new User.Builder(task.getResult(), email)
                                    .setName(credential.getDisplayName())
                                    .setPhotoUri(credential.getProfilePictureUri())
                                    .build()));
                        } else {
                            setResult(Resource.forFailure(task.getException()));
                        }
                    });
        } catch (ApiException e) {
            Log.e(TAG, "getSignInCredentialFromIntent failed", e);
            setResult(Resource.forFailure(e));
        }
    }
}
