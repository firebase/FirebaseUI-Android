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

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Represents an error in which the phone number couldn't be automatically verified and must
 * therefore be manually verified by the client by sending an SMS code.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneNumberVerificationRequiredException extends FirebaseUiException {
    private final String mPhoneNumber;

    /**
     * @param number the phone number requiring verification, formatted with a country code prefix
     */
    public PhoneNumberVerificationRequiredException(@NonNull String number) {
        super(ErrorCodes.PROVIDER_ERROR, "Phone number requires verification.");
        mPhoneNumber = number;
    }

    /**
     * @return the phone number requiring verification
     */
    @NonNull
    public String getPhoneNumber() {
        return mPhoneNumber;
    }
}
