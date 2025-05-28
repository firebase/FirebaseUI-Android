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

package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SignInViewModelBase extends AuthViewModelBase<IdpResponse> {
    protected SignInViewModelBase(Application application) {
        super(application);
    }

    @Override
    public void setResult(Resource<IdpResponse> output) {
        super.setResult(output);
    }

    public void handleSuccess(@NonNull IdpResponse response, @NonNull AuthResult result) {
        setResult(Resource.forSuccess(response.withResult(result)));
    }

    protected void handleMergeFailure(@NonNull AuthCredential credential) {
        IdpResponse failureResponse
                = new IdpResponse.Builder()
                .setPendingCredential(credential)
                .build();
        handleMergeFailure(failureResponse);
    }

    protected void handleMergeFailure(@NonNull IdpResponse failureResponse) {
        setResult(Resource.forFailure(new FirebaseAuthAnonymousUpgradeException(
                ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT,
                failureResponse)));
    }

}
