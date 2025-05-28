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

/**
 * Base class for all FirebaseUI exceptions.
 */
public class FirebaseUiException extends Exception {
    private final int mErrorCode;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code) {
        this(code, ErrorCodes.toFriendlyMessage(code));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code, @NonNull String message) {
        super(message);
        mErrorCode = code;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code, @NonNull Throwable cause) {
        this(code, ErrorCodes.toFriendlyMessage(code), cause);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FirebaseUiException(@ErrorCodes.Code int code,
                               @NonNull String message,
                               @NonNull Throwable cause) {
        super(message, cause);
        mErrorCode = code;
    }

    /**
     * @return error code associated with this exception
     * @see com.firebase.ui.auth.ErrorCodes
     */
    @ErrorCodes.Code
    public final int getErrorCode() {
        return mErrorCode;
    }
}
