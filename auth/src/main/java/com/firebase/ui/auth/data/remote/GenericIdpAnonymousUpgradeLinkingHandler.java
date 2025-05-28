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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.OAuthCredential;
import com.google.firebase.auth.OAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GenericIdpAnonymousUpgradeLinkingHandler extends GenericIdpSignInHandler {

    public GenericIdpAnonymousUpgradeLinkingHandler(Application application) {
        super(application);
    }

    @Override
    public void startSignIn(@NonNull FirebaseAuth auth,
                            @NonNull HelperActivityBase activity,
                            @NonNull String providerId) {
        setResult(Resource.forLoading());

        FlowParameters flowParameters = activity.getFlowParams();
        OAuthProvider provider = buildOAuthProvider(providerId, auth);
        if (flowParameters != null
                && AuthOperationManager.getInstance().canUpgradeAnonymous(auth, flowParameters)) {
            handleAnonymousUpgradeLinkingFlow(activity, provider, flowParameters);
            return;
        }

        handleNormalSignInFlow(auth, activity, provider);
    }

    private void handleAnonymousUpgradeLinkingFlow(final HelperActivityBase activity,
                                                   final OAuthProvider provider,
                                                   final FlowParameters flowParameters) {
        final boolean useEmulator = activity.getAuthUI().isUseEmulator();
        AuthOperationManager.getInstance().safeGenericIdpSignIn(activity, provider, flowParameters)
                .addOnSuccessListener(authResult -> {
                    // Pass the credential so we can sign-in on the after the merge
                    // conflict is resolved.
                    handleSuccess(
                            useEmulator,
                            provider.getProviderId(),
                            authResult.getUser(), (OAuthCredential) authResult.getCredential(),
                            /* setPendingCredential= */true);
                })
                .addOnFailureListener(e -> setResult(Resource.forFailure(e)));

    }
}
