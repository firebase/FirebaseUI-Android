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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PhoneNumberUtils {
    private static final int DEFAULT_COUNTRY_CODE_INT = 1;
    private static final String DEFAULT_COUNTRY_CODE = String.valueOf(DEFAULT_COUNTRY_CODE_INT);
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final CountryInfo DEFAULT_COUNTRY =
            new CountryInfo(DEFAULT_LOCALE, DEFAULT_COUNTRY_CODE_INT);

    private static final int MAX_COUNTRIES = 291;
    private static final int MAX_COUNTRY_CODES = 286;
    private static final int MAX_LENGTH_COUNTRY_CODE = 3;

    private static final Map<Integer, List<String>> CountryCodeToRegionCodeMap;
    private static final Map<String, Integer> CountryCodeByIsoMap;

    static {
        CountryCodeToRegionCodeMap = Collections.unmodifiableMap(createCountryCodeToRegionCodeMap
                ());
        CountryCodeByIsoMap = Collections.unmodifiableMap(createCountryCodeByIsoMap());
    }

    /**
     * This method may be used to force initialize the static members in the class.
     * It is recommended to do this in the background since the HashMaps created above
     * may be time consuming operations on some devices.
     */
    static void load() {
    }

    /**
     * This method works as follow:
     * <ol><li>When the android version is LOLLIPOP or greater, the reliable
     * {{@link android.telephony.PhoneNumberUtils#formatNumberToE164}} is used to format.</li>
     * <li>For lower versions, we construct a value with the input phone number stripped of
     * non numeric characters and prefix it with a "+" and country code</li>
     * </ol>

     * @param phoneNumber that may or may not itself have country code
     * @param countryInfo must have locale with ISO 3166 2-letter code for country
     * @return
     */
    @Nullable
    static String formatPhoneNumber(@NonNull String phoneNumber, @NonNull CountryInfo countryInfo) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return android.telephony.PhoneNumberUtils
                    .formatNumberToE164(phoneNumber, countryInfo.locale.getCountry());
        }
        return phoneNumber.startsWith("+")
                ? phoneNumber
                : ("+" + String.valueOf(countryInfo.countryCode)
                        + phoneNumber.replaceAll("[^\\d.]", ""));
    }


    /**
     * This method uses the country returned by  {@link #getCurrentCountryInfo(Context)} to
     * format the phone number. Internall invokes {@link #formatPhoneNumber(String, CountryInfo)}
     * @param phoneNumber that may or may not itself have country code
     * @return
     */

    @Nullable
    static String formatPhoneNumberUsingCurrentCountry(
            @NonNull String phoneNumber, Context context) {
        final CountryInfo currentCountry = PhoneNumberUtils.getCurrentCountryInfo(context);
        return formatPhoneNumber(phoneNumber, currentCountry);
    }

    @NonNull
    static CountryInfo getCurrentCountryInfo(@NonNull Context context) {
        Locale locale = getSimBasedLocale(context);

        if (locale == null) {
            locale = getOSLocale();
        }

        if (locale == null) {
            return DEFAULT_COUNTRY;
        }

        Integer countryCode = PhoneNumberUtils.getCountryCode(locale.getCountry());

        return countryCode == null ? DEFAULT_COUNTRY : new CountryInfo(locale, countryCode);
    }

    /**
     * This method should not be called on UI thread. Potentially creates a country code by iso
     * map which can take long in some devices
     * @param providedPhoneNumber works best when formatted as e164
     *
     * @return an instance of the PhoneNumber using the SIM information
     */

    protected static PhoneNumber getPhoneNumber(@NonNull String providedPhoneNumber) {
        String countryCode = DEFAULT_COUNTRY_CODE;
        String countryIso = DEFAULT_LOCALE.getCountry();

        String phoneNumber = providedPhoneNumber;
        if (providedPhoneNumber.startsWith("+")) {
            countryCode = countryCodeForPhoneNumber(providedPhoneNumber);
            countryIso = countryIsoForCountryCode(countryCode);
            phoneNumber = stripCountryCode(providedPhoneNumber, countryCode);
        }
        return new PhoneNumber(phoneNumber, countryIso, countryCode);
    }

    private static String countryIsoForCountryCode(String countryCode) {
        final List<String> countries = CountryCodeToRegionCodeMap.get(Integer.valueOf(countryCode));
        if (countries != null) {
            return countries.get(0);
        }
        return DEFAULT_LOCALE.getCountry();
    }

    /**
     * Country code extracted using shortest matching prefix like libPhoneNumber. See:
     * https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com
     * /google/i18n/phonenumbers/PhoneNumberUtil.java#L2395
     */
    private static String countryCodeForPhoneNumber(String normalizedPhoneNumber) {
        final String phoneWithoutPlusPrefix = normalizedPhoneNumber
                .replaceFirst("^\\+", "");
        final int numberLength = phoneWithoutPlusPrefix.length();

        for (int i = 1; i <= MAX_LENGTH_COUNTRY_CODE && i <= numberLength; i++) {
            final String potentialCountryCode = phoneWithoutPlusPrefix.substring(0, i);
            final Integer countryCodeKey = Integer.valueOf(potentialCountryCode);

            if (CountryCodeToRegionCodeMap.containsKey(countryCodeKey)) {
                return potentialCountryCode;
            }
        }

        return DEFAULT_COUNTRY_CODE;
    }

    private static Map<Integer, List<String>> createCountryCodeToRegionCodeMap() {
        final Map<Integer, List<String>> countryCodeToRegionCodeMap = new ConcurrentHashMap<>
                (MAX_COUNTRY_CODES);

        ArrayList<String> listWithRegionCode;

        listWithRegionCode = new ArrayList<>(25);
        listWithRegionCode.add("US");
        listWithRegionCode.add("AG");
        listWithRegionCode.add("AI");
        listWithRegionCode.add("AS");
        listWithRegionCode.add("BB");
        listWithRegionCode.add("BM");
        listWithRegionCode.add("BS");
        listWithRegionCode.add("CA");
        listWithRegionCode.add("DM");
        listWithRegionCode.add("DO");
        listWithRegionCode.add("GD");
        listWithRegionCode.add("GU");
        listWithRegionCode.add("JM");
        listWithRegionCode.add("KN");
        listWithRegionCode.add("KY");
        listWithRegionCode.add("LC");
        listWithRegionCode.add("MP");
        listWithRegionCode.add("MS");
        listWithRegionCode.add("PR");
        listWithRegionCode.add("SX");
        listWithRegionCode.add("TC");
        listWithRegionCode.add("TT");
        listWithRegionCode.add("VC");
        listWithRegionCode.add("VG");
        listWithRegionCode.add("VI");
        countryCodeToRegionCodeMap.put(1, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("RU");
        listWithRegionCode.add("KZ");
        countryCodeToRegionCodeMap.put(7, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("EG");
        countryCodeToRegionCodeMap.put(20, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ZA");
        countryCodeToRegionCodeMap.put(27, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GR");
        countryCodeToRegionCodeMap.put(30, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NL");
        countryCodeToRegionCodeMap.put(31, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BE");
        countryCodeToRegionCodeMap.put(32, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("FR");
        countryCodeToRegionCodeMap.put(33, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ES");
        countryCodeToRegionCodeMap.put(34, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("HU");
        countryCodeToRegionCodeMap.put(36, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IT");
        countryCodeToRegionCodeMap.put(39, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("RO");
        countryCodeToRegionCodeMap.put(40, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CH");
        countryCodeToRegionCodeMap.put(41, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AT");
        countryCodeToRegionCodeMap.put(43, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(4);
        listWithRegionCode.add("GB");
        listWithRegionCode.add("GG");
        listWithRegionCode.add("IM");
        listWithRegionCode.add("JE");
        countryCodeToRegionCodeMap.put(44, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("DK");
        countryCodeToRegionCodeMap.put(45, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SE");
        countryCodeToRegionCodeMap.put(46, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("NO");
        listWithRegionCode.add("SJ");
        countryCodeToRegionCodeMap.put(47, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PL");
        countryCodeToRegionCodeMap.put(48, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("DE");
        countryCodeToRegionCodeMap.put(49, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PE");
        countryCodeToRegionCodeMap.put(51, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MX");
        countryCodeToRegionCodeMap.put(52, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CU");
        countryCodeToRegionCodeMap.put(53, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AR");
        countryCodeToRegionCodeMap.put(54, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BR");
        countryCodeToRegionCodeMap.put(55, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CL");
        countryCodeToRegionCodeMap.put(56, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CO");
        countryCodeToRegionCodeMap.put(57, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("VE");
        countryCodeToRegionCodeMap.put(58, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MY");
        countryCodeToRegionCodeMap.put(60, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(3);
        listWithRegionCode.add("AU");
        listWithRegionCode.add("CC");
        listWithRegionCode.add("CX");
        countryCodeToRegionCodeMap.put(61, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ID");
        countryCodeToRegionCodeMap.put(62, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PH");
        countryCodeToRegionCodeMap.put(63, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NZ");
        countryCodeToRegionCodeMap.put(64, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SG");
        countryCodeToRegionCodeMap.put(65, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TH");
        countryCodeToRegionCodeMap.put(66, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("JP");
        countryCodeToRegionCodeMap.put(81, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KR");
        countryCodeToRegionCodeMap.put(82, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("VN");
        countryCodeToRegionCodeMap.put(84, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CN");
        countryCodeToRegionCodeMap.put(86, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TR");
        countryCodeToRegionCodeMap.put(90, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IN");
        countryCodeToRegionCodeMap.put(91, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PK");
        countryCodeToRegionCodeMap.put(92, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AF");
        countryCodeToRegionCodeMap.put(93, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LK");
        countryCodeToRegionCodeMap.put(94, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MM");
        countryCodeToRegionCodeMap.put(95, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IR");
        countryCodeToRegionCodeMap.put(98, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SS");
        countryCodeToRegionCodeMap.put(211, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("MA");
        listWithRegionCode.add("EH");
        countryCodeToRegionCodeMap.put(212, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("DZ");
        countryCodeToRegionCodeMap.put(213, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TN");
        countryCodeToRegionCodeMap.put(216, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LY");
        countryCodeToRegionCodeMap.put(218, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GM");
        countryCodeToRegionCodeMap.put(220, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SN");
        countryCodeToRegionCodeMap.put(221, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MR");
        countryCodeToRegionCodeMap.put(222, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ML");
        countryCodeToRegionCodeMap.put(223, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GN");
        countryCodeToRegionCodeMap.put(224, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CI");
        countryCodeToRegionCodeMap.put(225, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BF");
        countryCodeToRegionCodeMap.put(226, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NE");
        countryCodeToRegionCodeMap.put(227, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TG");
        countryCodeToRegionCodeMap.put(228, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BJ");
        countryCodeToRegionCodeMap.put(229, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MU");
        countryCodeToRegionCodeMap.put(230, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LR");
        countryCodeToRegionCodeMap.put(231, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SL");
        countryCodeToRegionCodeMap.put(232, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GH");
        countryCodeToRegionCodeMap.put(233, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NG");
        countryCodeToRegionCodeMap.put(234, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TD");
        countryCodeToRegionCodeMap.put(235, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CF");
        countryCodeToRegionCodeMap.put(236, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CM");
        countryCodeToRegionCodeMap.put(237, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CV");
        countryCodeToRegionCodeMap.put(238, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ST");
        countryCodeToRegionCodeMap.put(239, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GQ");
        countryCodeToRegionCodeMap.put(240, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GA");
        countryCodeToRegionCodeMap.put(241, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CG");
        countryCodeToRegionCodeMap.put(242, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CD");
        countryCodeToRegionCodeMap.put(243, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AO");
        countryCodeToRegionCodeMap.put(244, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GW");
        countryCodeToRegionCodeMap.put(245, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IO");
        countryCodeToRegionCodeMap.put(246, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AC");
        countryCodeToRegionCodeMap.put(247, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SC");
        countryCodeToRegionCodeMap.put(248, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SD");
        countryCodeToRegionCodeMap.put(249, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("RW");
        countryCodeToRegionCodeMap.put(250, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ET");
        countryCodeToRegionCodeMap.put(251, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SO");
        countryCodeToRegionCodeMap.put(252, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("DJ");
        countryCodeToRegionCodeMap.put(253, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KE");
        countryCodeToRegionCodeMap.put(254, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TZ");
        countryCodeToRegionCodeMap.put(255, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("UG");
        countryCodeToRegionCodeMap.put(256, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BI");
        countryCodeToRegionCodeMap.put(257, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MZ");
        countryCodeToRegionCodeMap.put(258, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ZM");
        countryCodeToRegionCodeMap.put(260, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MG");
        countryCodeToRegionCodeMap.put(261, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("RE");
        listWithRegionCode.add("YT");
        countryCodeToRegionCodeMap.put(262, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ZW");
        countryCodeToRegionCodeMap.put(263, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NA");
        countryCodeToRegionCodeMap.put(264, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MW");
        countryCodeToRegionCodeMap.put(265, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LS");
        countryCodeToRegionCodeMap.put(266, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BW");
        countryCodeToRegionCodeMap.put(267, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SZ");
        countryCodeToRegionCodeMap.put(268, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KM");
        countryCodeToRegionCodeMap.put(269, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("SH");
        listWithRegionCode.add("TA");
        countryCodeToRegionCodeMap.put(290, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ER");
        countryCodeToRegionCodeMap.put(291, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AW");
        countryCodeToRegionCodeMap.put(297, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("FO");
        countryCodeToRegionCodeMap.put(298, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GL");
        countryCodeToRegionCodeMap.put(299, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GI");
        countryCodeToRegionCodeMap.put(350, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PT");
        countryCodeToRegionCodeMap.put(351, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LU");
        countryCodeToRegionCodeMap.put(352, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IE");
        countryCodeToRegionCodeMap.put(353, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IS");
        countryCodeToRegionCodeMap.put(354, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AL");
        countryCodeToRegionCodeMap.put(355, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MT");
        countryCodeToRegionCodeMap.put(356, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CY");
        countryCodeToRegionCodeMap.put(357, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("FI");
        listWithRegionCode.add("AX");
        countryCodeToRegionCodeMap.put(358, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BG");
        countryCodeToRegionCodeMap.put(359, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LT");
        countryCodeToRegionCodeMap.put(370, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LV");
        countryCodeToRegionCodeMap.put(371, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("EE");
        countryCodeToRegionCodeMap.put(372, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MD");
        countryCodeToRegionCodeMap.put(373, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AM");
        countryCodeToRegionCodeMap.put(374, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BY");
        countryCodeToRegionCodeMap.put(375, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AD");
        countryCodeToRegionCodeMap.put(376, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MC");
        countryCodeToRegionCodeMap.put(377, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SM");
        countryCodeToRegionCodeMap.put(378, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("VA");
        countryCodeToRegionCodeMap.put(379, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("UA");
        countryCodeToRegionCodeMap.put(380, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("RS");
        countryCodeToRegionCodeMap.put(381, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("ME");
        countryCodeToRegionCodeMap.put(382, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("HR");
        countryCodeToRegionCodeMap.put(385, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SI");
        countryCodeToRegionCodeMap.put(386, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BA");
        countryCodeToRegionCodeMap.put(387, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MK");
        countryCodeToRegionCodeMap.put(389, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CZ");
        countryCodeToRegionCodeMap.put(420, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SK");
        countryCodeToRegionCodeMap.put(421, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LI");
        countryCodeToRegionCodeMap.put(423, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("FK");
        countryCodeToRegionCodeMap.put(500, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BZ");
        countryCodeToRegionCodeMap.put(501, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GT");
        countryCodeToRegionCodeMap.put(502, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SV");
        countryCodeToRegionCodeMap.put(503, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("HN");
        countryCodeToRegionCodeMap.put(504, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NI");
        countryCodeToRegionCodeMap.put(505, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CR");
        countryCodeToRegionCodeMap.put(506, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PA");
        countryCodeToRegionCodeMap.put(507, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PM");
        countryCodeToRegionCodeMap.put(508, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("HT");
        countryCodeToRegionCodeMap.put(509, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(3);
        listWithRegionCode.add("GP");
        listWithRegionCode.add("BL");
        listWithRegionCode.add("MF");
        countryCodeToRegionCodeMap.put(590, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BO");
        countryCodeToRegionCodeMap.put(591, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GY");
        countryCodeToRegionCodeMap.put(592, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("EC");
        countryCodeToRegionCodeMap.put(593, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GF");
        countryCodeToRegionCodeMap.put(594, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PY");
        countryCodeToRegionCodeMap.put(595, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MQ");
        countryCodeToRegionCodeMap.put(596, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SR");
        countryCodeToRegionCodeMap.put(597, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("UY");
        countryCodeToRegionCodeMap.put(598, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(2);
        listWithRegionCode.add("CW");
        listWithRegionCode.add("BQ");
        countryCodeToRegionCodeMap.put(599, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TL");
        countryCodeToRegionCodeMap.put(670, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NF");
        countryCodeToRegionCodeMap.put(672, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BN");
        countryCodeToRegionCodeMap.put(673, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NR");
        countryCodeToRegionCodeMap.put(674, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PG");
        countryCodeToRegionCodeMap.put(675, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TO");
        countryCodeToRegionCodeMap.put(676, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SB");
        countryCodeToRegionCodeMap.put(677, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("VU");
        countryCodeToRegionCodeMap.put(678, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("FJ");
        countryCodeToRegionCodeMap.put(679, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PW");
        countryCodeToRegionCodeMap.put(680, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("WF");
        countryCodeToRegionCodeMap.put(681, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("CK");
        countryCodeToRegionCodeMap.put(682, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NU");
        countryCodeToRegionCodeMap.put(683, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("WS");
        countryCodeToRegionCodeMap.put(685, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KI");
        countryCodeToRegionCodeMap.put(686, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NC");
        countryCodeToRegionCodeMap.put(687, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TV");
        countryCodeToRegionCodeMap.put(688, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PF");
        countryCodeToRegionCodeMap.put(689, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TK");
        countryCodeToRegionCodeMap.put(690, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("FM");
        countryCodeToRegionCodeMap.put(691, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MH");
        countryCodeToRegionCodeMap.put(692, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(800, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(808, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KP");
        countryCodeToRegionCodeMap.put(850, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("HK");
        countryCodeToRegionCodeMap.put(852, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MO");
        countryCodeToRegionCodeMap.put(853, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KH");
        countryCodeToRegionCodeMap.put(855, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LA");
        countryCodeToRegionCodeMap.put(856, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(870, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(878, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BD");
        countryCodeToRegionCodeMap.put(880, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(881, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(882, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(883, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TW");
        countryCodeToRegionCodeMap.put(886, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(888, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MV");
        countryCodeToRegionCodeMap.put(960, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("LB");
        countryCodeToRegionCodeMap.put(961, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("JO");
        countryCodeToRegionCodeMap.put(962, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SY");
        countryCodeToRegionCodeMap.put(963, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IQ");
        countryCodeToRegionCodeMap.put(964, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KW");
        countryCodeToRegionCodeMap.put(965, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("SA");
        countryCodeToRegionCodeMap.put(966, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("YE");
        countryCodeToRegionCodeMap.put(967, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("OM");
        countryCodeToRegionCodeMap.put(968, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("PS");
        countryCodeToRegionCodeMap.put(970, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AE");
        countryCodeToRegionCodeMap.put(971, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("IL");
        countryCodeToRegionCodeMap.put(972, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BH");
        countryCodeToRegionCodeMap.put(973, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("QA");
        countryCodeToRegionCodeMap.put(974, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("BT");
        countryCodeToRegionCodeMap.put(975, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("MN");
        countryCodeToRegionCodeMap.put(976, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("NP");
        countryCodeToRegionCodeMap.put(977, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("001");
        countryCodeToRegionCodeMap.put(979, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TJ");
        countryCodeToRegionCodeMap.put(992, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("TM");
        countryCodeToRegionCodeMap.put(993, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("AZ");
        countryCodeToRegionCodeMap.put(994, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("GE");
        countryCodeToRegionCodeMap.put(995, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("KG");
        countryCodeToRegionCodeMap.put(996, listWithRegionCode);

        listWithRegionCode = new ArrayList<>(1);
        listWithRegionCode.add("UZ");
        countryCodeToRegionCodeMap.put(998, listWithRegionCode);

        return countryCodeToRegionCodeMap;
    }

    private static synchronized Map<String, Integer> createCountryCodeByIsoMap() {
        final Map<String, Integer> countryCodeByIso = new HashMap<>(MAX_COUNTRIES);
        countryCodeByIso.put("AF", 93);
        countryCodeByIso.put("AX", 358);
        countryCodeByIso.put("AL", 355);
        countryCodeByIso.put("DZ", 213);
        countryCodeByIso.put("AS", 1);
        countryCodeByIso.put("AD", 376);
        countryCodeByIso.put("AO", 244);
        countryCodeByIso.put("AI", 1);
        countryCodeByIso.put("AG", 1);
        countryCodeByIso.put("AR", 54);
        countryCodeByIso.put("AM", 374);
        countryCodeByIso.put("AW", 297);
        countryCodeByIso.put("AC", 247);
        countryCodeByIso.put("AU", 61);
        countryCodeByIso.put("AT", 43);
        countryCodeByIso.put("AZ", 994);
        countryCodeByIso.put("BS", 1);
        countryCodeByIso.put("BH", 973);
        countryCodeByIso.put("BD", 880);
        countryCodeByIso.put("BB", 1);
        countryCodeByIso.put("BY", 375);
        countryCodeByIso.put("BE", 32);
        countryCodeByIso.put("BZ", 501);
        countryCodeByIso.put("BJ", 229);
        countryCodeByIso.put("BM", 1);
        countryCodeByIso.put("BT", 975);
        countryCodeByIso.put("BO", 591);
        countryCodeByIso.put("BA", 387);
        countryCodeByIso.put("BW", 267);
        countryCodeByIso.put("BR", 55);
        countryCodeByIso.put("IO", 246);
        countryCodeByIso.put("VG", 1);
        countryCodeByIso.put("BN", 673);
        countryCodeByIso.put("BG", 359);
        countryCodeByIso.put("BF", 226);
        countryCodeByIso.put("BI", 257);
        countryCodeByIso.put("KH", 855);
        countryCodeByIso.put("CM", 237);
        countryCodeByIso.put("CA", 1);
        countryCodeByIso.put("CV", 238);
        countryCodeByIso.put("BQ", 599);
        countryCodeByIso.put("KY", 1);
        countryCodeByIso.put("CF", 236);
        countryCodeByIso.put("TD", 235);
        countryCodeByIso.put("CL", 56);
        countryCodeByIso.put("CN", 86);
        countryCodeByIso.put("CX", 61);
        countryCodeByIso.put("CC", 61);
        countryCodeByIso.put("CO", 57);
        countryCodeByIso.put("KM", 269);
        countryCodeByIso.put("CD", 243);
        countryCodeByIso.put("CG", 242);
        countryCodeByIso.put("CK", 682);
        countryCodeByIso.put("CR", 506);
        countryCodeByIso.put("CI", 225);
        countryCodeByIso.put("HR", 385);
        countryCodeByIso.put("CU", 53);
        countryCodeByIso.put("CW", 599);
        countryCodeByIso.put("CY", 357);
        countryCodeByIso.put("CZ", 420);
        countryCodeByIso.put("DK", 45);
        countryCodeByIso.put("DJ", 253);
        countryCodeByIso.put("DM", 1);
        countryCodeByIso.put("DO", 1);
        countryCodeByIso.put("TL", 670);
        countryCodeByIso.put("EC", 593);
        countryCodeByIso.put("EG", 20);
        countryCodeByIso.put("SV", 503);
        countryCodeByIso.put("GQ", 240);
        countryCodeByIso.put("ER", 291);
        countryCodeByIso.put("EE", 372);
        countryCodeByIso.put("ET", 251);
        countryCodeByIso.put("FK", 500);
        countryCodeByIso.put("FO", 298);
        countryCodeByIso.put("FJ", 679);
        countryCodeByIso.put("FI", 358);
        countryCodeByIso.put("FR", 33);
        countryCodeByIso.put("GF", 594);
        countryCodeByIso.put("PF", 689);
        countryCodeByIso.put("GA", 241);
        countryCodeByIso.put("GM", 220);
        countryCodeByIso.put("GE", 995);
        countryCodeByIso.put("DE", 49);
        countryCodeByIso.put("GH", 233);
        countryCodeByIso.put("GI", 350);
        countryCodeByIso.put("GR", 30);
        countryCodeByIso.put("GL", 299);
        countryCodeByIso.put("GD", 1);
        countryCodeByIso.put("GP", 590);
        countryCodeByIso.put("GU", 1);
        countryCodeByIso.put("GT", 502);
        countryCodeByIso.put("GG", 44);
        countryCodeByIso.put("GN", 224);
        countryCodeByIso.put("GW", 245);
        countryCodeByIso.put("GY", 592);
        countryCodeByIso.put("HT", 509);
        countryCodeByIso.put("HM", 672);
        countryCodeByIso.put("HN", 504);
        countryCodeByIso.put("HK", 852);
        countryCodeByIso.put("HU", 36);
        countryCodeByIso.put("IS", 354);
        countryCodeByIso.put("IN", 91);
        countryCodeByIso.put("ID", 62);
        countryCodeByIso.put("IR", 98);
        countryCodeByIso.put("IQ", 964);
        countryCodeByIso.put("IE", 353);
        countryCodeByIso.put("IM", 44);
        countryCodeByIso.put("IL", 972);
        countryCodeByIso.put("IT", 39);
        countryCodeByIso.put("JM", 1);
        countryCodeByIso.put("JP", 81);
        countryCodeByIso.put("JE", 44);
        countryCodeByIso.put("JO", 962);
        countryCodeByIso.put("KZ", 7);
        countryCodeByIso.put("KE", 254);
        countryCodeByIso.put("KI", 686);
        countryCodeByIso.put("XK", 381);
        countryCodeByIso.put("KW", 965);
        countryCodeByIso.put("KG", 996);
        countryCodeByIso.put("LA", 856);
        countryCodeByIso.put("LV", 371);
        countryCodeByIso.put("LB", 961);
        countryCodeByIso.put("LS", 266);
        countryCodeByIso.put("LR", 231);
        countryCodeByIso.put("LY", 218);
        countryCodeByIso.put("LI", 423);
        countryCodeByIso.put("LT", 370);
        countryCodeByIso.put("LU", 352);
        countryCodeByIso.put("MO", 853);
        countryCodeByIso.put("MK", 389);
        countryCodeByIso.put("MG", 261);
        countryCodeByIso.put("MW", 265);
        countryCodeByIso.put("MY", 60);
        countryCodeByIso.put("MV", 960);
        countryCodeByIso.put("ML", 223);
        countryCodeByIso.put("MT", 356);
        countryCodeByIso.put("MH", 692);
        countryCodeByIso.put("MQ", 596);
        countryCodeByIso.put("MR", 222);
        countryCodeByIso.put("MU", 230);
        countryCodeByIso.put("YT", 262);
        countryCodeByIso.put("MX", 52);
        countryCodeByIso.put("FM", 691);
        countryCodeByIso.put("MD", 373);
        countryCodeByIso.put("MC", 377);
        countryCodeByIso.put("MN", 976);
        countryCodeByIso.put("ME", 382);
        countryCodeByIso.put("MS", 1);
        countryCodeByIso.put("MA", 212);
        countryCodeByIso.put("MZ", 258);
        countryCodeByIso.put("MM", 95);
        countryCodeByIso.put("NA", 264);
        countryCodeByIso.put("NR", 674);
        countryCodeByIso.put("NP", 977);
        countryCodeByIso.put("NL", 31);
        countryCodeByIso.put("NC", 687);
        countryCodeByIso.put("NZ", 64);
        countryCodeByIso.put("NI", 505);
        countryCodeByIso.put("NE", 227);
        countryCodeByIso.put("NG", 234);
        countryCodeByIso.put("NU", 683);
        countryCodeByIso.put("NF", 672);
        countryCodeByIso.put("KP", 850);
        countryCodeByIso.put("MP", 1);
        countryCodeByIso.put("NO", 47);
        countryCodeByIso.put("OM", 968);
        countryCodeByIso.put("PK", 92);
        countryCodeByIso.put("PW", 680);
        countryCodeByIso.put("PS", 970);
        countryCodeByIso.put("PA", 507);
        countryCodeByIso.put("PG", 675);
        countryCodeByIso.put("PY", 595);
        countryCodeByIso.put("PE", 51);
        countryCodeByIso.put("PH", 63);
        countryCodeByIso.put("PL", 48);
        countryCodeByIso.put("PT", 351);
        countryCodeByIso.put("PR", 1);
        countryCodeByIso.put("QA", 974);
        countryCodeByIso.put("RE", 262);
        countryCodeByIso.put("RO", 40);
        countryCodeByIso.put("RU", 7);
        countryCodeByIso.put("RW", 250);
        countryCodeByIso.put("BL", 590);
        countryCodeByIso.put("SH", 290);
        countryCodeByIso.put("KN", 1);
        countryCodeByIso.put("LC", 1);
        countryCodeByIso.put("MF", 590);
        countryCodeByIso.put("PM", 508);
        countryCodeByIso.put("VC", 1);
        countryCodeByIso.put("WS", 685);
        countryCodeByIso.put("SM", 378);
        countryCodeByIso.put("ST", 239);
        countryCodeByIso.put("SA", 966);
        countryCodeByIso.put("SN", 221);
        countryCodeByIso.put("RS", 381);
        countryCodeByIso.put("SC", 248);
        countryCodeByIso.put("SL", 232);
        countryCodeByIso.put("SG", 65);
        countryCodeByIso.put("SX", 1);
        countryCodeByIso.put("SK", 421);
        countryCodeByIso.put("SI", 386);
        countryCodeByIso.put("SB", 677);
        countryCodeByIso.put("SO", 252);
        countryCodeByIso.put("ZA", 27);
        countryCodeByIso.put("GS", 500);
        countryCodeByIso.put("KR", 82);
        countryCodeByIso.put("SS", 211);
        countryCodeByIso.put("ES", 34);
        countryCodeByIso.put("LK", 94);
        countryCodeByIso.put("SD", 249);
        countryCodeByIso.put("SR", 597);
        countryCodeByIso.put("SJ", 47);
        countryCodeByIso.put("SZ", 268);
        countryCodeByIso.put("SE", 46);
        countryCodeByIso.put("CH", 41);
        countryCodeByIso.put("SY", 963);
        countryCodeByIso.put("TW", 886);
        countryCodeByIso.put("TJ", 992);
        countryCodeByIso.put("TZ", 255);
        countryCodeByIso.put("TH", 66);
        countryCodeByIso.put("TG", 228);
        countryCodeByIso.put("TK", 690);
        countryCodeByIso.put("TO", 676);
        countryCodeByIso.put("TT", 1);
        countryCodeByIso.put("TN", 216);
        countryCodeByIso.put("TR", 90);
        countryCodeByIso.put("TM", 993);
        countryCodeByIso.put("TC", 1);
        countryCodeByIso.put("TV", 688);
        countryCodeByIso.put("VI", 1);
        countryCodeByIso.put("UG", 256);
        countryCodeByIso.put("UA", 380);
        countryCodeByIso.put("AE", 971);
        countryCodeByIso.put("GB", 44);
        countryCodeByIso.put("US", 1);
        countryCodeByIso.put("UY", 598);
        countryCodeByIso.put("UZ", 998);
        countryCodeByIso.put("VU", 678);
        countryCodeByIso.put("VA", 379);
        countryCodeByIso.put("VE", 58);
        countryCodeByIso.put("VN", 84);
        countryCodeByIso.put("WF", 681);
        countryCodeByIso.put("EH", 212);
        countryCodeByIso.put("YE", 967);
        countryCodeByIso.put("ZM", 260);
        countryCodeByIso.put("ZW", 263);
        return countryCodeByIso;
    }

    @Nullable
    public static Integer getCountryCode(String countryIso) {
        return countryIso == null
                ? null
                : CountryCodeByIsoMap.get(countryIso.toUpperCase(Locale.getDefault()));
    }

    private static String stripCountryCode(String phoneNumber, String countryCode) {
        return phoneNumber.replaceFirst("^\\+?" + countryCode, "");
    }

    private static Locale getSimBasedLocale(@NonNull Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final String countryIso = tm != null ? tm.getSimCountryIso() : null;
        return TextUtils.isEmpty(countryIso) ? null : new Locale("", countryIso);
    }

    private static Locale getOSLocale() {
        return Locale.getDefault();
    }
}
