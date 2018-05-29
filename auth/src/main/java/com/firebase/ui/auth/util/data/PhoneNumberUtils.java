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
package com.firebase.ui.auth.util.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;

import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.data.model.PhoneNumber;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class PhoneNumberUtils {
    private static final int DEFAULT_COUNTRY_CODE_INT = 1;
    private static final String DEFAULT_COUNTRY_CODE = String.valueOf(DEFAULT_COUNTRY_CODE_INT);
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final CountryInfo DEFAULT_COUNTRY =
            new CountryInfo(DEFAULT_LOCALE, DEFAULT_COUNTRY_CODE_INT);

    private static final int MAX_COUNTRY_CODES = 215;
    private static final int MAX_COUNTRIES = 248;
    private static final int MAX_LENGTH_COUNTRY_CODE = 3;

    private static final SparseArray<List<String>> COUNTRY_TO_REGION_CODES =
            createCountryCodeToRegionCodeMap();

    private static Map<String, Integer> COUNTRY_TO_ISO_CODES;

    /**
     * This method works as follow: <ol><li>When the android version is LOLLIPOP or greater, the
     * reliable {{@link android.telephony.PhoneNumberUtils#formatNumberToE164}} is used to
     * format.</li> <li>For lower versions, we construct a value with the input phone number
     * stripped of non numeric characters and prefix it with a "+" and country code</li> </ol>
     *
     * @param phoneNumber that may or may not itself have country code
     * @param countryInfo must have locale with ISO 3166 2-letter code for country
     */
    public static String format(@NonNull String phoneNumber, @NonNull CountryInfo countryInfo) {
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        } else {
            return "+"
                    + String.valueOf(countryInfo.getCountryCode())
                    + phoneNumber.replaceAll("[^\\d.]", "");
        }
    }

    /**
     * This method uses the country returned by  {@link #getCurrentCountryInfo(Context)} to format
     * the phone number. Internally invokes {@link #format(String, CountryInfo)}
     *
     * @param phoneNumber that may or may not itself have country code
     */
    @Nullable
    public static String formatUsingCurrentCountry(@NonNull String phoneNumber, Context context) {
        return format(phoneNumber, getCurrentCountryInfo(context));
    }

    @NonNull
    public static CountryInfo getCurrentCountryInfo(@NonNull Context context) {
        Locale locale = getSimBasedLocale(context);

        if (locale == null) {
            locale = getOSLocale();
        }

        if (locale == null) {
            return DEFAULT_COUNTRY;
        }

        Integer countryCode = getCountryCode(locale.getCountry());

        return countryCode == null ? DEFAULT_COUNTRY : new CountryInfo(locale, countryCode);
    }

    /**
     * This method should not be called on UI thread. Potentially creates a country code by iso map
     * which can take long in some devices
     *
     * @param providedPhoneNumber works best when formatted as e164
     * @return an instance of the PhoneNumber using the SIM information
     */
    public static PhoneNumber getPhoneNumber(@NonNull String providedPhoneNumber) {
        String countryCode = DEFAULT_COUNTRY_CODE;
        String countryIso = DEFAULT_LOCALE.getCountry();

        String phoneNumber = providedPhoneNumber;
        if (providedPhoneNumber.startsWith("+")) {
            countryCode = getCountryCodeForPhoneNumberOrDefault(providedPhoneNumber);
            countryIso = getCountryIsoForCountryCode(countryCode);
            phoneNumber = stripCountryCode(providedPhoneNumber, countryCode);
        }

        return new PhoneNumber(phoneNumber, countryIso, countryCode);
    }

    public static boolean isValid(@NonNull String number) {
        return number.startsWith("+") && getCountryCodeForPhoneNumber(number) != null;
    }

    public static boolean isValidIso(@Nullable String iso) {
        return getCountryCode(iso) != null;
    }

    /**
     * @see #getPhoneNumber(String)
     */
    public static PhoneNumber getPhoneNumber(
            @NonNull String providedCountryIso, @NonNull String providedNationalNumber) {
        Integer countryCode = getCountryCode(providedCountryIso);
        if (countryCode == null) {
            // Invalid ISO supplied:
            countryCode = DEFAULT_COUNTRY_CODE_INT;
            providedCountryIso = DEFAULT_COUNTRY_CODE;
        }

        // National number shouldn't include '+', but just in case:
        providedNationalNumber = stripPlusSign(providedNationalNumber);

        return new PhoneNumber(
                providedNationalNumber, providedCountryIso, String.valueOf(countryCode));
    }

    @Nullable
    public static Integer getCountryCode(String countryIso) {
        if (COUNTRY_TO_ISO_CODES == null) {
            initCountryCodeByIsoMap();
        }
        return countryIso == null
                ? null : COUNTRY_TO_ISO_CODES.get(countryIso.toUpperCase(Locale.getDefault()));
    }

    public static Map<String, Integer> getImmutableCountryIsoMap() {
        if (COUNTRY_TO_ISO_CODES == null) {
            initCountryCodeByIsoMap();
        }
        return COUNTRY_TO_ISO_CODES;
    }

    private static String getCountryIsoForCountryCode(String countryCode) {
        List<String> countries = COUNTRY_TO_REGION_CODES.get(Integer.parseInt(countryCode));
        if (countries != null) {
            return countries.get(0);
        }
        return DEFAULT_LOCALE.getCountry();
    }

    @Nullable
    public static List<String> getCountryIsosFromCountryCode(String countryCode) {
        return !isValid(countryCode) ? null :
                COUNTRY_TO_REGION_CODES.get(Integer.parseInt(countryCode.substring(1)));
    }

    /**
     * Country code extracted using shortest matching prefix like libPhoneNumber. See:
     * https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com
     * /google/i18n/phonenumbers/PhoneNumberUtil.java#L2395
     */
    @Nullable
    private static String getCountryCodeForPhoneNumber(String normalizedPhoneNumber) {
        String phoneWithoutPlusPrefix = normalizedPhoneNumber.replaceFirst("^\\+", "");
        int numberLength = phoneWithoutPlusPrefix.length();

        for (int i = 1; i <= MAX_LENGTH_COUNTRY_CODE && i <= numberLength; i++) {
            String potentialCountryCode = phoneWithoutPlusPrefix.substring(0, i);
            Integer countryCodeKey = Integer.valueOf(potentialCountryCode);

            if (COUNTRY_TO_REGION_CODES.indexOfKey(countryCodeKey) >= 0) {
                return potentialCountryCode;
            }
        }
        return null;
    }

    @NonNull
    private static String getCountryCodeForPhoneNumberOrDefault(String normalizedPhoneNumber) {
        String code = getCountryCodeForPhoneNumber(normalizedPhoneNumber);
        return code == null ? DEFAULT_COUNTRY_CODE : code;
    }

    private static String stripCountryCode(String phoneNumber, String countryCode) {
        return phoneNumber.replaceFirst("^\\+?" + countryCode, "");
    }

    private static String stripPlusSign(String phoneNumber) {
        return phoneNumber.replaceFirst("^\\+?", "");
    }

    private static Locale getSimBasedLocale(@NonNull Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryIso = tm != null ? tm.getSimCountryIso() : null;
        return TextUtils.isEmpty(countryIso) ? null : new Locale("", countryIso);
    }

    private static Locale getOSLocale() {
        return Locale.getDefault();
    }

    private static SparseArray<List<String>> createCountryCodeToRegionCodeMap() {
        SparseArray<List<String>> map = new SparseArray<>(MAX_COUNTRY_CODES);

        map.put(1, asList(
                "US", "AG", "AI", "AS", "BB", "BM", "BS", "CA", "DM", "DO", "GD", "GU", "JM", "KN",
                "KY", "LC", "MP", "MS", "PR", "SX", "TC", "TT", "VC", "VG", "VI"));
        map.put(7, asList("RU", "KZ"));
        map.put(20, singletonList("EG"));
        map.put(27, singletonList("ZA"));
        map.put(30, singletonList("GR"));
        map.put(31, singletonList("NL"));
        map.put(32, singletonList("BE"));
        map.put(33, singletonList("FR"));
        map.put(34, singletonList("ES"));
        map.put(36, singletonList("HU"));
        map.put(39, singletonList("IT"));
        map.put(40, singletonList("RO"));
        map.put(41, singletonList("CH"));
        map.put(43, singletonList("AT"));
        map.put(44, asList("GB", "GG", "IM", "JE"));
        map.put(45, singletonList("DK"));
        map.put(46, singletonList("SE"));
        map.put(47, asList("NO", "SJ"));
        map.put(48, singletonList("PL"));
        map.put(49, singletonList("DE"));
        map.put(51, singletonList("PE"));
        map.put(52, singletonList("MX"));
        map.put(53, singletonList("CU"));
        map.put(54, singletonList("AR"));
        map.put(55, singletonList("BR"));
        map.put(56, singletonList("CL"));
        map.put(57, singletonList("CO"));
        map.put(58, singletonList("VE"));
        map.put(60, singletonList("MY"));
        map.put(61, asList("AU", "CC", "CX"));
        map.put(62, singletonList("ID"));
        map.put(63, singletonList("PH"));
        map.put(64, singletonList("NZ"));
        map.put(65, singletonList("SG"));
        map.put(66, singletonList("TH"));
        map.put(81, singletonList("JP"));
        map.put(82, singletonList("KR"));
        map.put(84, singletonList("VN"));
        map.put(86, singletonList("CN"));
        map.put(90, singletonList("TR"));
        map.put(91, singletonList("IN"));
        map.put(92, singletonList("PK"));
        map.put(93, singletonList("AF"));
        map.put(94, singletonList("LK"));
        map.put(95, singletonList("MM"));
        map.put(98, singletonList("IR"));
        map.put(211, singletonList("SS"));
        map.put(212, asList("MA", "EH"));
        map.put(213, singletonList("DZ"));
        map.put(216, singletonList("TN"));
        map.put(218, singletonList("LY"));
        map.put(220, singletonList("GM"));
        map.put(221, singletonList("SN"));
        map.put(222, singletonList("MR"));
        map.put(223, singletonList("ML"));
        map.put(224, singletonList("GN"));
        map.put(225, singletonList("CI"));
        map.put(226, singletonList("BF"));
        map.put(227, singletonList("NE"));
        map.put(228, singletonList("TG"));
        map.put(229, singletonList("BJ"));
        map.put(230, singletonList("MU"));
        map.put(231, singletonList("LR"));
        map.put(232, singletonList("SL"));
        map.put(233, singletonList("GH"));
        map.put(234, singletonList("NG"));
        map.put(235, singletonList("TD"));
        map.put(236, singletonList("CF"));
        map.put(237, singletonList("CM"));
        map.put(238, singletonList("CV"));
        map.put(239, singletonList("ST"));
        map.put(240, singletonList("GQ"));
        map.put(241, singletonList("GA"));
        map.put(242, singletonList("CG"));
        map.put(243, singletonList("CD"));
        map.put(244, singletonList("AO"));
        map.put(245, singletonList("GW"));
        map.put(246, singletonList("IO"));
        map.put(247, singletonList("AC"));
        map.put(248, singletonList("SC"));
        map.put(249, singletonList("SD"));
        map.put(250, singletonList("RW"));
        map.put(251, singletonList("ET"));
        map.put(252, singletonList("SO"));
        map.put(253, singletonList("DJ"));
        map.put(254, singletonList("KE"));
        map.put(255, singletonList("TZ"));
        map.put(256, singletonList("UG"));
        map.put(257, singletonList("BI"));
        map.put(258, singletonList("MZ"));
        map.put(260, singletonList("ZM"));
        map.put(261, singletonList("MG"));
        map.put(262, asList("RE", "YT"));
        map.put(263, singletonList("ZW"));
        map.put(264, singletonList("NA"));
        map.put(265, singletonList("MW"));
        map.put(266, singletonList("LS"));
        map.put(267, singletonList("BW"));
        map.put(268, singletonList("SZ"));
        map.put(269, singletonList("KM"));
        map.put(290, asList("SH", "TA"));
        map.put(291, singletonList("ER"));
        map.put(297, singletonList("AW"));
        map.put(298, singletonList("FO"));
        map.put(299, singletonList("GL"));
        map.put(350, singletonList("GI"));
        map.put(351, singletonList("PT"));
        map.put(352, singletonList("LU"));
        map.put(353, singletonList("IE"));
        map.put(354, singletonList("IS"));
        map.put(355, singletonList("AL"));
        map.put(356, singletonList("MT"));
        map.put(357, singletonList("CY"));
        map.put(358, asList("FI", "AX"));
        map.put(359, singletonList("BG"));
        map.put(370, singletonList("LT"));
        map.put(371, singletonList("LV"));
        map.put(372, singletonList("EE"));
        map.put(373, singletonList("MD"));
        map.put(374, singletonList("AM"));
        map.put(375, singletonList("BY"));
        map.put(376, singletonList("AD"));
        map.put(377, singletonList("MC"));
        map.put(378, singletonList("SM"));
        map.put(379, singletonList("VA"));
        map.put(380, singletonList("UA"));
        map.put(381, singletonList("RS"));
        map.put(382, singletonList("ME"));
        map.put(385, singletonList("HR"));
        map.put(386, singletonList("SI"));
        map.put(387, singletonList("BA"));
        map.put(389, singletonList("MK"));
        map.put(420, singletonList("CZ"));
        map.put(421, singletonList("SK"));
        map.put(423, singletonList("LI"));
        map.put(500, singletonList("FK"));
        map.put(501, singletonList("BZ"));
        map.put(502, singletonList("GT"));
        map.put(503, singletonList("SV"));
        map.put(504, singletonList("HN"));
        map.put(505, singletonList("NI"));
        map.put(506, singletonList("CR"));
        map.put(507, singletonList("PA"));
        map.put(508, singletonList("PM"));
        map.put(509, singletonList("HT"));
        map.put(590, asList("GP", "BL", "MF"));
        map.put(591, singletonList("BO"));
        map.put(592, singletonList("GY"));
        map.put(593, singletonList("EC"));
        map.put(594, singletonList("GF"));
        map.put(595, singletonList("PY"));
        map.put(596, singletonList("MQ"));
        map.put(597, singletonList("SR"));
        map.put(598, singletonList("UY"));
        map.put(599, asList("CW", "BQ"));
        map.put(670, singletonList("TL"));
        map.put(672, singletonList("NF"));
        map.put(673, singletonList("BN"));
        map.put(674, singletonList("NR"));
        map.put(675, singletonList("PG"));
        map.put(676, singletonList("TO"));
        map.put(677, singletonList("SB"));
        map.put(678, singletonList("VU"));
        map.put(679, singletonList("FJ"));
        map.put(680, singletonList("PW"));
        map.put(681, singletonList("WF"));
        map.put(682, singletonList("CK"));
        map.put(683, singletonList("NU"));
        map.put(685, singletonList("WS"));
        map.put(686, singletonList("KI"));
        map.put(687, singletonList("NC"));
        map.put(688, singletonList("TV"));
        map.put(689, singletonList("PF"));
        map.put(690, singletonList("TK"));
        map.put(691, singletonList("FM"));
        map.put(692, singletonList("MH"));
        map.put(800, singletonList("001"));
        map.put(808, singletonList("001"));
        map.put(850, singletonList("KP"));
        map.put(852, singletonList("HK"));
        map.put(853, singletonList("MO"));
        map.put(855, singletonList("KH"));
        map.put(856, singletonList("LA"));
        map.put(870, singletonList("001"));
        map.put(878, singletonList("001"));
        map.put(880, singletonList("BD"));
        map.put(881, singletonList("001"));
        map.put(882, singletonList("001"));
        map.put(883, singletonList("001"));
        map.put(886, singletonList("TW"));
        map.put(888, singletonList("001"));
        map.put(960, singletonList("MV"));
        map.put(961, singletonList("LB"));
        map.put(962, singletonList("JO"));
        map.put(963, singletonList("SY"));
        map.put(964, singletonList("IQ"));
        map.put(965, singletonList("KW"));
        map.put(966, singletonList("SA"));
        map.put(967, singletonList("YE"));
        map.put(968, singletonList("OM"));
        map.put(970, singletonList("PS"));
        map.put(971, singletonList("AE"));
        map.put(972, singletonList("IL"));
        map.put(973, singletonList("BH"));
        map.put(974, singletonList("QA"));
        map.put(975, singletonList("BT"));
        map.put(976, singletonList("MN"));
        map.put(977, singletonList("NP"));
        map.put(979, singletonList("001"));
        map.put(992, singletonList("TJ"));
        map.put(993, singletonList("TM"));
        map.put(994, singletonList("AZ"));
        map.put(995, singletonList("GE"));
        map.put(996, singletonList("KG"));
        map.put(998, singletonList("UZ"));

        return map;
    }

    private static void initCountryCodeByIsoMap() {
        Map<String, Integer> map = new HashMap<>(MAX_COUNTRIES);

        for (int i = 0; i < COUNTRY_TO_REGION_CODES.size(); i++) {
            int code = COUNTRY_TO_REGION_CODES.keyAt(i);
            List<String> regions = COUNTRY_TO_REGION_CODES.get(code);

            for (String region : regions) {
                if (region.equals("001")) { continue; }
                if (map.containsKey(region)) {
                    throw new IllegalStateException("Duplicate regions for country code: " + code);
                }

                map.put(region, code);
            }
        }

        // TODO Figure out why these exceptions exist.
        // This map used to be hardcoded so this is the diff from the generated version.
        map.remove("TA");
        map.put("HM", 672);
        map.put("GS", 500);
        map.put("XK", 381);

        COUNTRY_TO_ISO_CODES = Collections.unmodifiableMap(map);
    }
}
