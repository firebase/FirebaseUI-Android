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

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.ContinueUrlBuilder;
import com.firebase.ui.auth.util.data.EmailLinkPersistenceManager;
import com.firebase.ui.auth.util.data.SessionUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class EmailLinkSendEmailHandler extends AuthViewModelBase<String> {
    private static final int SESSION_ID_LENGTH = 10;

    public EmailLinkSendEmailHandler(Application application) {
        super(application);
    }

    public void sendSignInLinkToEmail(@NonNull final String email,
                                      @NonNull final ActionCodeSettings actionCodeSettings,
                                      @Nullable final IdpResponse idpResponseForLinking,
                                      final boolean forceSameDevice) {
        if (getAuth() == null) {
            return;
        }
        setResult(Resource.forLoading());

        final String anonymousUserId =
                AuthOperationManager.getInstance().canUpgradeAnonymous(getAuth(), getArguments())
                ? getAuth().getCurrentUser().getUid() : null;
        final String sessionId =
                SessionUtils.generateRandomAlphaNumericString(SESSION_ID_LENGTH);

        ActionCodeSettings mutatedSettings = addSessionInfoToActionCodeSettings(actionCodeSettings,
                sessionId, anonymousUserId, idpResponseForLinking, forceSameDevice);

        getAuth().sendSignInLinkToEmail(email, mutatedSettings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        EmailLinkPersistenceManager.getInstance().saveEmail(getApplication(),
                                email, sessionId, anonymousUserId);

                        setResult(Resource.forSuccess(email));
                    } else {
                        setResult(Resource.forFailure(task.getException()));
                    }
                });
    }

    private ActionCodeSettings addSessionInfoToActionCodeSettings(@NonNull ActionCodeSettings
                                                                          actionCodeSettings,
                                                                  @NonNull String sessionId,
                                                                  @NonNull String anonymousUserId,
                                                                  @Nullable IdpResponse response,
                                                                  boolean forceSameDevice) {

        String continueUrl = actionCodeSettings.getUrl();
        ContinueUrlBuilder continueUrlBuilder = new ContinueUrlBuilder(continueUrl);
        continueUrlBuilder.appendSessionId(sessionId);
        continueUrlBuilder.appendAnonymousUserId(anonymousUserId);
        continueUrlBuilder.appendForceSameDeviceBit(forceSameDevice);
        if (response != null) {
            continueUrlBuilder.appendProviderId(response.getProviderType());
        }

        return ActionCodeSettings.newBuilder()
                .setUrl(continueUrlBuilder.build())
                .setHandleCodeInApp(true)
                .setAndroidPackageName(actionCodeSettings.getAndroidPackageName(),
                        actionCodeSettings.getAndroidInstallApp(),
                        actionCodeSettings.getAndroidMinimumVersion())
                .setIOSBundleId(actionCodeSettings.getIOSBundle())
                .build();
    }
}
