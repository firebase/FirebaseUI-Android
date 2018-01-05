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

import com.firebase.ui.auth.data.model.CountryInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

@RunWith(RobolectricTestRunner.class)
public class CountryInfoTests {
    private static final Locale COUNTRY_NAME_US = new Locale("", "US");
    private static final int COUNTRY_CODE_US = 1;
    private static final Locale COUNTRY_NAME_BS = new Locale("", "BS");
    private static final int COUNTRY_CODE_JP = 81;

    @Test
    public void testEquals_differentObject() {
        final CountryInfo countryInfo1 = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);
        final CountryInfo countryInfo2 = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);

        assertEquals(countryInfo1, countryInfo2);
        assertEquals(countryInfo2, countryInfo1);
        assertEquals(countryInfo1, countryInfo1);
        assertEquals(countryInfo2, countryInfo2);
        assertNotSame(countryInfo2, countryInfo1);
    }

    @Test
    public void testEquals_null() {
        final CountryInfo countryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);

        assertFalse(countryInfo.equals(null));
    }

    @Test
    public void testEquals_differentClass() {
        final CountryInfo countryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);

        assertFalse(countryInfo.equals(0));
    }

    @Test
    public void testEquals_differentCountryName() {
        final CountryInfo usCountryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);
        final CountryInfo bsCountryInfo = new CountryInfo(COUNTRY_NAME_BS, COUNTRY_CODE_US);

        assertFalse(usCountryInfo.equals(bsCountryInfo));
    }

    @Test
    public void testEquals_nullCountryName() {
        final CountryInfo usCountryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);
        final CountryInfo bsCountryInfo = new CountryInfo(null, COUNTRY_CODE_US);

        assertFalse(usCountryInfo.equals(bsCountryInfo));
        assertFalse(bsCountryInfo.equals(usCountryInfo));
    }

    @Test
    public void testEquals_differentCountryCode() {
        final CountryInfo usCountryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);
        final CountryInfo jpCountryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_JP);

        assertFalse(usCountryInfo.equals(jpCountryInfo));
    }

    @Test
    public void testHashCode() {
        final CountryInfo usCountryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);
        final CountryInfo bsCountryInfo = new CountryInfo(null, COUNTRY_CODE_US);

        assertEquals(2611999, usCountryInfo.hashCode());
        assertEquals(1, bsCountryInfo.hashCode());
    }

    @Test
    public void testToString() {
        final CountryInfo usCountryInfo = new CountryInfo(COUNTRY_NAME_US, COUNTRY_CODE_US);
        int firstLetter = 'U' - 0x41 + 0x1F1E6;
        int secondLetter = 'S' - 0x41 + 0x1F1E6;
        String expected = new String(Character.toChars(firstLetter))
                + new String(Character.toChars(secondLetter))
                + " "
                + usCountryInfo.getLocale().getDisplayCountry()
                + " +" + usCountryInfo.getCountryCode();
        assertEquals(expected, usCountryInfo.toString());
    }
}
