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

package com.firebase.ui.auth.viewmodel.phone;

import android.app.Application;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneProviderResponseHandler extends SignInViewModelBase {
    public PhoneProviderResponseHandler(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull PhoneAuthCredential credential,
                            @NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.forFailure(response.getError()));
            return;
        }
        if (!response.getProviderType().equals(PhoneAuthProvider.PROVIDER_ID)) {
            throw new IllegalStateException(
                    "This handler cannot be used without a phone response.");
        }

        setResult(Resource.forLoading());

        AuthOperationManager.getInstance()
                .signInAndLinkWithCredential(getAuth(), getArguments(), credential)
                .addOnSuccessListener(result -> handleSuccess(response, result))
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        // With phone auth, this only happens if we are trying to upgrade
                        // an anonymous account using a phone number that is already registered
                        // on another account
                        handleMergeFailure(((FirebaseAuthUserCollisionException) e).getUpdatedCredential());
                    } else {
                        setResult(Resource.forFailure(e));
                    }
                });
    }
}
