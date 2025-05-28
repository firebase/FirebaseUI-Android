/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.data.remote;

import android.net.Uri;
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

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
                .continueWithTask(task1 -> Tasks.forResult(authResult));
    }
}
