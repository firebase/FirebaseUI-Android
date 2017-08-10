package com.firebase.ui.auth.util.accountlink;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.User;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Merges an existing account's profile with the new user's profile.
 * <p>
 * <b>Note:</b> This operation always returns a successful task to minimize login interruptions.
 */
public class ProfileMerger implements Continuation<AuthResult, Task<AuthResult>> {
    private static final String TAG = "ProfileMerger";

    private final IdpResponse mIdpResponse;

    public ProfileMerger(IdpResponse response) {
        mIdpResponse = response;
    }

    @Override
    public Task<AuthResult> then(@NonNull Task<AuthResult> task) throws Exception {
        final AuthResult authResult = task.getResult();
        FirebaseUser firebaseUser = authResult.getUser();

        String name = firebaseUser.getDisplayName();
        Uri photoUri = firebaseUser.getPhotoUrl();
        if (!TextUtils.isEmpty(name) && photoUri != null) {
            return Tasks.forResult(authResult);
        }

        User user = mIdpResponse.getUser();
        if (TextUtils.isEmpty(name)) { name = user.getName(); }
        if (photoUri == null) { photoUri = user.getPhotoUri(); }

        return firebaseUser.updateProfile(
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .setPhotoUri(photoUri)
                        .build())
                .addOnFailureListener(new TaskFailureLogger(TAG, "Error updating profile"))
                .continueWithTask(new Continuation<Void, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<Void> task) throws Exception {
                        return Tasks.forResult(authResult);
                    }
                });
    }
}
