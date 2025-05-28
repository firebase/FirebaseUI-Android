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

import android.app.Application;
import android.content.Intent;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailSignInHandler extends SingleProviderSignInHandler<Void> {
    public EmailSignInHandler(Application application) {
        super(application, EmailAuthProvider.PROVIDER_ID);
    }

    @Override
    public void startSignIn(@NonNull FirebaseAuth auth,
                            @NonNull HelperActivityBase activity,
                            @NonNull String providerId) {
        activity.startActivityForResult(
                EmailActivity.createIntent(activity, activity.getFlowParams()),
                RequestCodes.EMAIL_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
            // The activity deals with this case. This conflict is handled by the developer.
        } else if (requestCode == RequestCodes.EMAIL_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                setResult(Resource.forFailure(new UserCancellationException()));
            } else {
                setResult(Resource.forSuccess(response));
            }
        }
    }
}
