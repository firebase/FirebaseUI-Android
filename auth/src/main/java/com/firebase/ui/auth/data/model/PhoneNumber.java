/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2017 Google Inc
 */
package com.firebase.ui.auth.data.model;

import android.support.annotation.RestrictTo;
import android.text.TextUtils;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class PhoneNumber {
    private static final PhoneNumber EMPTY_PHONE_NUMBER = new PhoneNumber("", "", "");

    private final String mPhoneNumber;
    private final String mCountryIso;
    private final String mCountryCode;

    public PhoneNumber(String phoneNumber, String countryIso, String countryCode) {
        mPhoneNumber = phoneNumber;
        mCountryIso = countryIso;
        mCountryCode = countryCode;
    }

    /**
     * Returns an empty instance of this class
     */
    public static PhoneNumber emptyPhone() {
        return EMPTY_PHONE_NUMBER;
    }

    public static boolean isValid(PhoneNumber phoneNumber) {
        return phoneNumber != null
                && !EMPTY_PHONE_NUMBER.equals(phoneNumber)
                && !TextUtils.isEmpty(phoneNumber.getPhoneNumber())
                && !TextUtils.isEmpty(phoneNumber.getCountryCode())
                && !TextUtils.isEmpty(phoneNumber.getCountryIso());
    }

    public static boolean isCountryValid(PhoneNumber phoneNumber) {
        return phoneNumber != null
                && !EMPTY_PHONE_NUMBER.equals(phoneNumber)
                && !TextUtils.isEmpty(phoneNumber.getCountryCode())
                && !TextUtils.isEmpty(phoneNumber.getCountryIso());
    }

    /**
     * Returns country code
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * Returns phone number without country code
     */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    /**
     * Returns 2 char country ISO
     */
    public String getCountryIso() {
        return mCountryIso;
    }
}
