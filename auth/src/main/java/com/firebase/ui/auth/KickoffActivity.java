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

package com.firebase.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.data.remote.SignInKickstarter;
import com.firebase.ui.auth.ui.InvisibleActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModelProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KickoffActivity extends InvisibleActivityBase {
    public static SignInKickstarter mKickstarter;

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createBaseIntent(context, KickoffActivity.class, flowParams);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKickstarter = new ViewModelProvider(this).get(SignInKickstarter.class);
        mKickstarter.init(getFlowParams());
        mKickstarter.getOperation().observe(this, new ResourceObserver<>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                finish(RESULT_OK, response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof UserCancellationException) {
                    finish(RESULT_CANCELED, null);
                } else if (e instanceof FirebaseAuthAnonymousUpgradeException) {
                    IdpResponse res = ((FirebaseAuthAnonymousUpgradeException) e).getResponse();
                    finish(RESULT_CANCELED, new Intent().putExtra(ExtraConstants.IDP_RESPONSE,
                            res));
                } else {
                    finish(RESULT_CANCELED, IdpResponse.getErrorIntent(e));
                }
            }
        });

        Task<Void> checkPlayServicesTask = getFlowParams().isPlayServicesRequired()
                ? GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
                : Tasks.forResult((Void) null);

        checkPlayServicesTask
                .addOnSuccessListener(this, aVoid -> {
                    if (savedInstanceState != null) {
                        return;
                    }

                    mKickstarter.start();
                })
                .addOnFailureListener(this, e -> finish(RESULT_CANCELED, IdpResponse.getErrorIntent(new FirebaseUiException(
                        ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED, e))));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.EMAIL_FLOW
                && (resultCode == RequestCodes.EMAIL_LINK_WRONG_DEVICE_FLOW
                || resultCode == RequestCodes.EMAIL_LINK_INVALID_LINK_FLOW)) {
            invalidateEmailLink();
        }

        mKickstarter.onActivityResult(requestCode, resultCode, data);
    }

    public void invalidateEmailLink() {
        FlowParameters flowParameters = getFlowParams();
        flowParameters.emailLink = null;
        setIntent(getIntent().putExtra(ExtraConstants.FLOW_PARAMS,
                flowParameters));
    }
}
