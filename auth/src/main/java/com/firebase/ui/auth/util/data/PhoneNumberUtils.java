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

public final class PhoneNumberUtils {
    private static final int DEFAULT_COUNTRY_CODE_INT = 1;
    private static final String DEFAULT_COUNTRY_CODE = String.valueOf(DEFAULT_COUNTRY_CODE_INT);
    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final CountryInfo DEFAULT_COUNTRY =
            new CountryInfo(DEFAULT_LOCALE, DEFAULT_COUNTRY_CODE_INT);

    private static final int MAX_COUNTRIES = 291;
    private static final int MAX_COUNTRY_CODES = 286;
    private static final int MAX_LENGTH_COUNTRY_CODE = 3;

    private static final SparseArray<List<String>> COUNTRY_TO_REGION_CODES =
            createCountryCodeToRegionCodeMap();
    private static final Map<String, Integer> COUNTRY_TO_ISO_CODES =
            Collections.unmodifiableMap(createCountryCodeByIsoMap());

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
            countryCode = countryCodeForPhoneNumber(providedPhoneNumber);
            countryIso = countryIsoForCountryCode(countryCode);
            phoneNumber = stripCountryCode(providedPhoneNumber, countryCode);
        }

        return new PhoneNumber(phoneNumber, countryIso, countryCode);
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
                providedNationalNumber,
                providedCountryIso,
                String.valueOf(countryCode));
    }

    @Nullable
    public static Integer getCountryCode(String countryIso) {
        return countryIso == null
                ? null
                : COUNTRY_TO_ISO_CODES.get(countryIso.toUpperCase(Locale.getDefault()));
    }

    private static String countryIsoForCountryCode(String countryCode) {
        List<String> countries = COUNTRY_TO_REGION_CODES.get(Integer.parseInt(countryCode));
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
        String phoneWithoutPlusPrefix = normalizedPhoneNumber.replaceFirst("^\\+", "");
        int numberLength = phoneWithoutPlusPrefix.length();

        for (int i = 1; i <= MAX_LENGTH_COUNTRY_CODE && i <= numberLength; i++) {
            String potentialCountryCode = phoneWithoutPlusPrefix.substring(0, i);
            Integer countryCodeKey = Integer.valueOf(potentialCountryCode);

            if (COUNTRY_TO_REGION_CODES.indexOfKey(countryCodeKey) >= 0) {
                return potentialCountryCode;
            }
        }

        return DEFAULT_COUNTRY_CODE;
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

    private static synchronized SparseArray<List<String>> createCountryCodeToRegionCodeMap() {
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

    private static synchronized Map<String, Integer> createCountryCodeByIsoMap() {
        Map<String, Integer> map = new HashMap<>(MAX_COUNTRIES);

        map.put("AF", 93);
        map.put("AX", 358);
        map.put("AL", 355);
        map.put("DZ", 213);
        map.put("AS", 1);
        map.put("AD", 376);
        map.put("AO", 244);
        map.put("AI", 1);
        map.put("AG", 1);
        map.put("AR", 54);
        map.put("AM", 374);
        map.put("AW", 297);
        map.put("AC", 247);
        map.put("AU", 61);
        map.put("AT", 43);
        map.put("AZ", 994);
        map.put("BS", 1);
        map.put("BH", 973);
        map.put("BD", 880);
        map.put("BB", 1);
        map.put("BY", 375);
        map.put("BE", 32);
        map.put("BZ", 501);
        map.put("BJ", 229);
        map.put("BM", 1);
        map.put("BT", 975);
        map.put("BO", 591);
        map.put("BA", 387);
        map.put("BW", 267);
        map.put("BR", 55);
        map.put("IO", 246);
        map.put("VG", 1);
        map.put("BN", 673);
        map.put("BG", 359);
        map.put("BF", 226);
        map.put("BI", 257);
        map.put("KH", 855);
        map.put("CM", 237);
        map.put("CA", 1);
        map.put("CV", 238);
        map.put("BQ", 599);
        map.put("KY", 1);
        map.put("CF", 236);
        map.put("TD", 235);
        map.put("CL", 56);
        map.put("CN", 86);
        map.put("CX", 61);
        map.put("CC", 61);
        map.put("CO", 57);
        map.put("KM", 269);
        map.put("CD", 243);
        map.put("CG", 242);
        map.put("CK", 682);
        map.put("CR", 506);
        map.put("CI", 225);
        map.put("HR", 385);
        map.put("CU", 53);
        map.put("CW", 599);
        map.put("CY", 357);
        map.put("CZ", 420);
        map.put("DK", 45);
        map.put("DJ", 253);
        map.put("DM", 1);
        map.put("DO", 1);
        map.put("TL", 670);
        map.put("EC", 593);
        map.put("EG", 20);
        map.put("SV", 503);
        map.put("GQ", 240);
        map.put("ER", 291);
        map.put("EE", 372);
        map.put("ET", 251);
        map.put("FK", 500);
        map.put("FO", 298);
        map.put("FJ", 679);
        map.put("FI", 358);
        map.put("FR", 33);
        map.put("GF", 594);
        map.put("PF", 689);
        map.put("GA", 241);
        map.put("GM", 220);
        map.put("GE", 995);
        map.put("DE", 49);
        map.put("GH", 233);
        map.put("GI", 350);
        map.put("GR", 30);
        map.put("GL", 299);
        map.put("GD", 1);
        map.put("GP", 590);
        map.put("GU", 1);
        map.put("GT", 502);
        map.put("GG", 44);
        map.put("GN", 224);
        map.put("GW", 245);
        map.put("GY", 592);
        map.put("HT", 509);
        map.put("HM", 672);
        map.put("HN", 504);
        map.put("HK", 852);
        map.put("HU", 36);
        map.put("IS", 354);
        map.put("IN", 91);
        map.put("ID", 62);
        map.put("IR", 98);
        map.put("IQ", 964);
        map.put("IE", 353);
        map.put("IM", 44);
        map.put("IL", 972);
        map.put("IT", 39);
        map.put("JM", 1);
        map.put("JP", 81);
        map.put("JE", 44);
        map.put("JO", 962);
        map.put("KZ", 7);
        map.put("KE", 254);
        map.put("KI", 686);
        map.put("XK", 381);
        map.put("KW", 965);
        map.put("KG", 996);
        map.put("LA", 856);
        map.put("LV", 371);
        map.put("LB", 961);
        map.put("LS", 266);
        map.put("LR", 231);
        map.put("LY", 218);
        map.put("LI", 423);
        map.put("LT", 370);
        map.put("LU", 352);
        map.put("MO", 853);
        map.put("MK", 389);
        map.put("MG", 261);
        map.put("MW", 265);
        map.put("MY", 60);
        map.put("MV", 960);
        map.put("ML", 223);
        map.put("MT", 356);
        map.put("MH", 692);
        map.put("MQ", 596);
        map.put("MR", 222);
        map.put("MU", 230);
        map.put("YT", 262);
        map.put("MX", 52);
        map.put("FM", 691);
        map.put("MD", 373);
        map.put("MC", 377);
        map.put("MN", 976);
        map.put("ME", 382);
        map.put("MS", 1);
        map.put("MA", 212);
        map.put("MZ", 258);
        map.put("MM", 95);
        map.put("NA", 264);
        map.put("NR", 674);
        map.put("NP", 977);
        map.put("NL", 31);
        map.put("NC", 687);
        map.put("NZ", 64);
        map.put("NI", 505);
        map.put("NE", 227);
        map.put("NG", 234);
        map.put("NU", 683);
        map.put("NF", 672);
        map.put("KP", 850);
        map.put("MP", 1);
        map.put("NO", 47);
        map.put("OM", 968);
        map.put("PK", 92);
        map.put("PW", 680);
        map.put("PS", 970);
        map.put("PA", 507);
        map.put("PG", 675);
        map.put("PY", 595);
        map.put("PE", 51);
        map.put("PH", 63);
        map.put("PL", 48);
        map.put("PT", 351);
        map.put("PR", 1);
        map.put("QA", 974);
        map.put("RE", 262);
        map.put("RO", 40);
        map.put("RU", 7);
        map.put("RW", 250);
        map.put("BL", 590);
        map.put("SH", 290);
        map.put("KN", 1);
        map.put("LC", 1);
        map.put("MF", 590);
        map.put("PM", 508);
        map.put("VC", 1);
        map.put("WS", 685);
        map.put("SM", 378);
        map.put("ST", 239);
        map.put("SA", 966);
        map.put("SN", 221);
        map.put("RS", 381);
        map.put("SC", 248);
        map.put("SL", 232);
        map.put("SG", 65);
        map.put("SX", 1);
        map.put("SK", 421);
        map.put("SI", 386);
        map.put("SB", 677);
        map.put("SO", 252);
        map.put("ZA", 27);
        map.put("GS", 500);
        map.put("KR", 82);
        map.put("SS", 211);
        map.put("ES", 34);
        map.put("LK", 94);
        map.put("SD", 249);
        map.put("SR", 597);
        map.put("SJ", 47);
        map.put("SZ", 268);
        map.put("SE", 46);
        map.put("CH", 41);
        map.put("SY", 963);
        map.put("TW", 886);
        map.put("TJ", 992);
        map.put("TZ", 255);
        map.put("TH", 66);
        map.put("TG", 228);
        map.put("TK", 690);
        map.put("TO", 676);
        map.put("TT", 1);
        map.put("TN", 216);
        map.put("TR", 90);
        map.put("TM", 993);
        map.put("TC", 1);
        map.put("TV", 688);
        map.put("VI", 1);
        map.put("UG", 256);
        map.put("UA", 380);
        map.put("AE", 971);
        map.put("GB", 44);
        map.put("US", 1);
        map.put("UY", 598);
        map.put("UZ", 998);
        map.put("VU", 678);
        map.put("VA", 379);
        map.put("VE", 58);
        map.put("VN", 84);
        map.put("WF", 681);
        map.put("EH", 212);
        map.put("YE", 967);
        map.put("ZM", 260);
        map.put("ZW", 263);

        return map;
    }
}
