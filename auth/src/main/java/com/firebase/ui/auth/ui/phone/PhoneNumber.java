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
package com.firebase.ui.auth.ui.phone;

import android.text.TextUtils;

final class PhoneNumber {
    private static final PhoneNumber EMPTY_PHONE_NUMBER = new PhoneNumber("", "", "");

    private final String phoneNumber;
    private final String countryIso;
    private final String countryCode;

    public PhoneNumber(String phoneNumber, String countryIso, String countryCode) {
        this.phoneNumber = phoneNumber;
        this.countryIso = countryIso;
        this.countryCode = countryCode;
    }

    /**
     * Returns an empty instance of this class
     */
    public static PhoneNumber emptyPhone() {
        return EMPTY_PHONE_NUMBER;
    }

    /**
     * Returns country code
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Returns phone number without country code
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns 2 char country ISO
     */
    public String getCountryIso() {
        return countryIso;
    }

    public static boolean isValid(PhoneNumber phoneNumber) {
        return phoneNumber != null && !EMPTY_PHONE_NUMBER.equals(phoneNumber) && !TextUtils
                .isEmpty(phoneNumber.getPhoneNumber()) && !TextUtils.isEmpty(phoneNumber
                .getCountryCode()) && !TextUtils.isEmpty(phoneNumber.getCountryIso());
    }

    public static boolean isCountryValid(PhoneNumber phoneNumber) {
        return phoneNumber != null && !EMPTY_PHONE_NUMBER.equals(phoneNumber) && !TextUtils
                .isEmpty(phoneNumber.getCountryCode()) && !TextUtils.isEmpty(phoneNumber
                .getCountryIso());
    }
}
