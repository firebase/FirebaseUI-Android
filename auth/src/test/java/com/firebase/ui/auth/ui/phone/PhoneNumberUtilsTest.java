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

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.RAW_PHONE;
import static org.junit.Assert.assertEquals;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class PhoneNumberUtilsTest {
    private static final String INVENTED_ISO = "random";

    @Test
    public void testGetPhoneNumber() throws Exception {
        final PhoneNumber number = PhoneNumberUtils.getPhoneNumber(RAW_PHONE);
        assertEquals(PhoneTestConstants.PHONE_NO_COUNTRY_CODE, number.getPhoneNumber());
        assertEquals(PhoneTestConstants.US_COUNTRY_CODE, number.getCountryCode());
        assertEquals(PhoneTestConstants.US_ISO2, number.getCountryIso());
    }

    @Test
    public void testGetPhoneNumber_withLongestCountryCode() throws Exception {
        final PhoneNumber phoneNumber = PhoneNumberUtils.getPhoneNumber(PhoneTestConstants
                .YE_RAW_PHONE);
        assertEquals(PhoneTestConstants.PHONE_NO_COUNTRY_CODE, phoneNumber.getPhoneNumber());
        assertEquals(PhoneTestConstants.YE_COUNTRY_CODE, phoneNumber.getCountryCode());
        assertEquals(PhoneTestConstants.YE_ISO2, phoneNumber.getCountryIso());
    }

    @Test
    public void testGetPhoneNumber_withPhoneWithoutPlusSign() throws Exception {
        final PhoneNumber phoneNumber = PhoneNumberUtils.getPhoneNumber(PhoneTestConstants.PHONE);
        assertEquals(PhoneTestConstants.PHONE, phoneNumber.getPhoneNumber());
        assertEquals(PhoneTestConstants.US_COUNTRY_CODE, phoneNumber.getCountryCode());
        assertEquals(PhoneTestConstants.US_ISO2, phoneNumber.getCountryIso());
    }

    @Test
    public void testGetPhoneNumber_noCountryCode() throws Exception {
        final PhoneNumber number = PhoneNumberUtils.getPhoneNumber("0" + PhoneTestConstants
                .PHONE_NO_COUNTRY_CODE);
        assertEquals("0" + PhoneTestConstants.PHONE_NO_COUNTRY_CODE, number.getPhoneNumber());
        assertEquals(PhoneTestConstants.US_COUNTRY_CODE, number.getCountryCode());
        assertEquals(PhoneTestConstants.US_ISO2, number.getCountryIso());
    }
}
