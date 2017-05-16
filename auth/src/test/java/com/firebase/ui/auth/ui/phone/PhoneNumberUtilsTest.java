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

import android.content.Context;
import android.telephony.TelephonyManager;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Locale;

import static com.firebase.ui.auth.ui.phone.PhoneNumberUtils.*;
import static com.firebase.ui.auth.ui.phone.PhoneTestConstants.RAW_PHONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class PhoneNumberUtilsTest {
    @Test
    public void testGetPhoneNumber() throws Exception {
        final PhoneNumber number = getPhoneNumber(RAW_PHONE);
        assertEquals(PhoneTestConstants.PHONE_NO_COUNTRY_CODE, number.getPhoneNumber());
        assertEquals(PhoneTestConstants.US_COUNTRY_CODE, number.getCountryCode());
        assertEquals(PhoneTestConstants.US_ISO2, number.getCountryIso());
    }

    @Test
    public void testGetPhoneNumber_withLongestCountryCode() throws Exception {
        final PhoneNumber phoneNumber = getPhoneNumber(PhoneTestConstants
                .YE_RAW_PHONE);
        assertEquals(PhoneTestConstants.PHONE_NO_COUNTRY_CODE, phoneNumber.getPhoneNumber());
        assertEquals(PhoneTestConstants.YE_COUNTRY_CODE, phoneNumber.getCountryCode());
        assertEquals(PhoneTestConstants.YE_ISO2, phoneNumber.getCountryIso());
    }

    @Test
    public void testGetPhoneNumber_withPhoneWithoutPlusSign() throws Exception {
        final PhoneNumber phoneNumber = getPhoneNumber(PhoneTestConstants.PHONE);
        assertEquals(PhoneTestConstants.PHONE, phoneNumber.getPhoneNumber());
        assertEquals(PhoneTestConstants.US_COUNTRY_CODE, phoneNumber.getCountryCode());
        assertEquals(PhoneTestConstants.US_ISO2, phoneNumber.getCountryIso());
    }

    @Test
    public void testGetPhoneNumber_noCountryCode() throws Exception {
        final PhoneNumber number = getPhoneNumber("0" + PhoneTestConstants
                .PHONE_NO_COUNTRY_CODE);
        assertEquals("0" + PhoneTestConstants.PHONE_NO_COUNTRY_CODE, number.getPhoneNumber());
        assertEquals(PhoneTestConstants.US_COUNTRY_CODE, number.getCountryCode());
        assertEquals(PhoneTestConstants.US_ISO2, number.getCountryIso());
    }

    @Test
    public void testGetCountryCode() throws Exception {
        assertEquals(new Integer(86), getCountryCode(Locale.CHINA.getCountry()));
        assertEquals(null, getCountryCode(null));
        assertEquals(null, getCountryCode(new Locale("", "DJJZ").getCountry()));
    }

    @Test
    @Config(constants = BuildConfig.class, sdk = 21)
    public void testFormatNumberToE164_aboveApi21() {
        String validPhoneNumber = "+919994947354";
        CountryInfo indiaCountryInfo = new CountryInfo(new Locale("", "IN"), 91);
        //no leading plus
        assertEquals(validPhoneNumber, formatPhoneNumber("9994947354", indiaCountryInfo));
        //no country code
        assertEquals(validPhoneNumber, formatPhoneNumber("919994947354", indiaCountryInfo));
        //fully formatted
        assertEquals(validPhoneNumber, formatPhoneNumber("+919994947354", indiaCountryInfo));
        //with hyphens
        assertEquals(validPhoneNumber, formatPhoneNumber("+91-(999)-(49)-(47354)", indiaCountryInfo));
        //with spaces leading plus
        assertEquals(validPhoneNumber, formatPhoneNumber("+91 99949 47354", indiaCountryInfo));
        // space formatting
        assertEquals(validPhoneNumber, formatPhoneNumber("91 99949 47354", indiaCountryInfo));
        // parantheses and hyphens
        assertEquals(validPhoneNumber, formatPhoneNumber("(99949) 47-354", indiaCountryInfo));
        // mismatched country
        assertEquals(validPhoneNumber, formatPhoneNumber("+919994947354",
                                                         new CountryInfo(
                                                                 new Locale("", "US"), 1)));
        // incorrect country with well formatted number
        assertNull(formatPhoneNumber("999474735", indiaCountryInfo));

        // incorrect country with unformattednumber
        assertNull(validPhoneNumber, formatPhoneNumber("919994947354",
                                                         new CountryInfo(
                                                                 new Locale("", "US"), 1)));
        //incorrect country, incorrect phone number
        assertNull(formatPhoneNumber("+914349873457", new CountryInfo(
                new Locale("", "US"), 1)));
    }

    @Test
    @Config(constants = BuildConfig.class, sdk = 16)
    public void testFormatNumberToE164_belowApi21() {
        String validPhoneNumber = "+919994947354";
        CountryInfo indiaCountryInfo = new CountryInfo(new Locale("", "IN"), 91);
        // no leading plus
        assertEquals(validPhoneNumber, formatPhoneNumber("9994947354", indiaCountryInfo));
        // fully formatted
        assertEquals(validPhoneNumber, formatPhoneNumber("+919994947354", indiaCountryInfo));
        // parantheses and hyphens
        assertEquals(validPhoneNumber, formatPhoneNumber("(99949) 47-354", indiaCountryInfo));

        // The following cases would fail for lower api versions.
        // Leaving tests in place to formally identify cases

        // no leading +
        // assertEquals(validPhoneNumber, formatPhoneNumber("919994947354", indiaCountryInfo));

        // with hyphens
        // assertEquals(validPhoneNumber, formatPhoneNumber("+91-(999)-(49)-(47354)",
        // indiaCountryInfo));

        // with spaces leading plus
        // assertEquals(validPhoneNumber, formatPhoneNumber("+91 99949 47354", indiaCountryInfo));

        // space formatting
        // assertEquals(validPhoneNumber, formatPhoneNumber("91 99949 47354", indiaCountryInfo));

        // invalid phone number
        // assertNull(formatPhoneNumber("999474735", indiaCountryInfo));
    }

    @Test
    public void testGetCurrentCountryInfo_fromSim() {
        Context context = mock(Context.class);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);

        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(telephonyManager.getSimCountryIso()).thenReturn("IN");
        assertEquals(new CountryInfo(new Locale("", "IN"), 91), getCurrentCountryInfo(context));
    }

    @Test
    public void testGetCurrentCountryInfo_noTelephonyReturnsDefaultLocale() {
        Context context = mock(Context.class);
        assertEquals(new CountryInfo(
                Locale.getDefault(),
                PhoneNumberUtils.getCountryCode(Locale.getDefault().getCountry())),
                getCurrentCountryInfo(context));
    }
}
