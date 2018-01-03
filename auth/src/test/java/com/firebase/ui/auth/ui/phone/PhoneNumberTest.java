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

import com.firebase.ui.auth.data.model.PhoneNumber;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PhoneNumberTest {
    @Test
    public void testIsValid_emptyPhone() {
        assertFalse(PhoneNumber.isValid(PhoneNumber.emptyPhone()));
    }

    @Test
    public void testIsValid_nullPhone() {
        assertFalse(PhoneNumber.isValid(null));
    }

    @Test
    public void testIsValid_emptyMembers() {
        PhoneNumber invalidPhoneNumber = new PhoneNumber(
                "",
                PhoneTestConstants.US_ISO2,
                PhoneTestConstants.US_COUNTRY_CODE);
        assertFalse(PhoneNumber.isValid(invalidPhoneNumber));
        invalidPhoneNumber = new PhoneNumber(PhoneTestConstants.PHONE, "", PhoneTestConstants
                .US_COUNTRY_CODE);
        assertFalse(PhoneNumber.isValid(invalidPhoneNumber));
        invalidPhoneNumber = new PhoneNumber(
                PhoneTestConstants.PHONE,
                PhoneTestConstants.US_ISO2,
                "");
        assertFalse(PhoneNumber.isValid(invalidPhoneNumber));
    }

    @Test
    public void testIsValid() {
        final PhoneNumber validPhoneNumber = new PhoneNumber(
                PhoneTestConstants.PHONE,
                PhoneTestConstants.US_ISO2,
                PhoneTestConstants.US_COUNTRY_CODE);
        assertTrue(PhoneNumber.isValid(validPhoneNumber));
    }

    @Test
    public void testIsCountryValid_emptyPhone() {
        assertFalse(PhoneNumber.isCountryValid(PhoneNumber.emptyPhone()));
    }

    @Test
    public void testIsCountryValid_nullPhone() {
        assertFalse(PhoneNumber.isCountryValid(null));
    }

    @Test
    public void testIsCountryValid_emptyMembers() {
        PhoneNumber invalidPhoneNumber = new PhoneNumber(
                "",
                "",
                PhoneTestConstants.US_COUNTRY_CODE);
        assertFalse(PhoneNumber.isCountryValid(invalidPhoneNumber));
        invalidPhoneNumber = new PhoneNumber("", PhoneTestConstants.US_ISO2, "");
        assertFalse(PhoneNumber.isCountryValid(invalidPhoneNumber));
    }

    @Test
    public void testIsCountryValid() {
        final PhoneNumber validPhoneNumber = new PhoneNumber(
                "",
                PhoneTestConstants.US_ISO2,
                PhoneTestConstants.US_COUNTRY_CODE);
        assertTrue(PhoneNumber.isCountryValid(validPhoneNumber));
    }
}
