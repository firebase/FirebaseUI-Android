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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseAuthAnonymousUpgradeException extends Exception {

    private IdpResponse mResponse;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseAuthAnonymousUpgradeException(@ErrorCodes.Code int code,
                                                 @NonNull IdpResponse response) {
        super(ErrorCodes.toFriendlyMessage(code));
        mResponse = response;
    }

    public IdpResponse getResponse() {
        return mResponse;
    }
}
