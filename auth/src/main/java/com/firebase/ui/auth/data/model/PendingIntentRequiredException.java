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

package com.firebase.ui.auth.data.model;

import android.app.PendingIntent;
import android.content.IntentSender;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PendingIntentRequiredException extends FirebaseUiException {
    private final PendingIntent mPendingIntent;
    private final IntentSender mIntentSender;
    private final int mRequestCode;

    /**
     * Constructor for cases when a PendingIntent is available.
     *
     * @param pendingIntent The PendingIntent required to complete the operation.
     * @param requestCode   The associated request code.
     */
    public PendingIntentRequiredException(@NonNull PendingIntent pendingIntent, int requestCode) {
        super(ErrorCodes.UNKNOWN_ERROR);
        mPendingIntent = pendingIntent;
        mIntentSender = null;
        mRequestCode = requestCode;
    }

    /**
     * Constructor for cases when an IntentSender is available.
     *
     * @param intentSender The IntentSender required to complete the operation.
     * @param requestCode  The associated request code.
     */
    public PendingIntentRequiredException(@NonNull IntentSender intentSender, int requestCode) {
        super(ErrorCodes.UNKNOWN_ERROR);
        mIntentSender = intentSender;
        mPendingIntent = null;
        mRequestCode = requestCode;
    }

    /**
     * Returns the PendingIntent, if available.
     *
     * @return The PendingIntent or null if not available.
     */
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    /**
     * Returns the IntentSender, if available.
     *
     * @return The IntentSender or null if not available.
     */
    public IntentSender getIntentSender() {
        return mIntentSender;
    }

    public int getRequestCode() {
        return mRequestCode;
    }
}
