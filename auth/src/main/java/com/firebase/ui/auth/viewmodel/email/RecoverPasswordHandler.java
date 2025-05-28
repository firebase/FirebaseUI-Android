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

package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordHandler extends AuthViewModelBase<String> {
    public RecoverPasswordHandler(Application application) {
        super(application);
    }

    public void startReset(@NonNull final String email, @Nullable ActionCodeSettings actionCodeSettings) {
        setResult(Resource.forLoading());
        Task<Void> reset = actionCodeSettings != null
                ? getAuth().sendPasswordResetEmail(email, actionCodeSettings)
                : getAuth().sendPasswordResetEmail(email);

        reset.addOnCompleteListener(task -> {
            Resource<String> resource = task.isSuccessful()
                    ? Resource.forSuccess(email)
                    : Resource.forFailure(task.getException());
            setResult(resource);
        });
    }
}
