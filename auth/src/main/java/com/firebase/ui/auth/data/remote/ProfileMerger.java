package com.firebase.ui.auth.data.remote;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.data.TaskFailureLogger;
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProfileMerger implements Continuation<AuthResult, Task<AuthResult>> {
    private static final String TAG = "ProfileMerger";

    private final IdpResponse mResponse;

    public ProfileMerger(IdpResponse response) {
        mResponse = response;
    }

    @Override
    public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
        final AuthResult authResult = task.getResult();
        FirebaseUser firebaseUser = authResult.getUser();

        String name = firebaseUser.getDisplayName();
        Uri photoUri = firebaseUser.getPhotoUrl();
        if (!TextUtils.isEmpty(name) && photoUri != null) {
            return Tasks.forResult(authResult);
        }

        User user = mResponse.getUser();
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
                    public Task<AuthResult> then(@NonNull Task<Void> task) {
                        return Tasks.forResult(authResult);
                    }
                });
    }
}
