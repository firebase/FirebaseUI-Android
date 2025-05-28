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

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link ActivityResultContract} describing that the caller can launch authentication flow with a
 * {@link Intent} and is guaranteed to receive a {@link FirebaseAuthUIAuthenticationResult} as
 * result. The given input intent <b>must</b> be created using a
 * {@link com.firebase.ui.auth.AuthUI.SignInIntentBuilder} in order to guarantee a successful
 * launch of the authentication flow.
 */
public class FirebaseAuthUIActivityResultContract extends
        ActivityResultContract<Intent, FirebaseAuthUIAuthenticationResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Intent input) {
        return input;
    }

    @Override
    @NonNull
    public FirebaseAuthUIAuthenticationResult parseResult(int resultCode, @Nullable Intent intent) {
        return new FirebaseAuthUIAuthenticationResult(resultCode, IdpResponse.fromResultIntent(intent));
    }

}
