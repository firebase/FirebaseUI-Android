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

package com.firebase.uidemo.util;

import android.content.Context;
import android.widget.Toast;

import com.firebase.uidemo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import androidx.annotation.NonNull;

/**
 * Notifies the user of sign in successes or failures beyond the lifecycle of an activity.
 */
public class SignInResultNotifier implements OnCompleteListener<AuthResult> {
    private Context mContext;

    public SignInResultNotifier(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {
        if (task.isSuccessful()) {
            Toast.makeText(mContext, R.string.signed_in, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, R.string.anonymous_auth_failed_msg, Toast.LENGTH_LONG).show();
        }
    }
}
