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

package com.firebase.ui.auth.data.client;

import android.os.AsyncTask;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.CountryInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// TODO We need to move away from ListView and AsyncTask in the future and use (say)
// RecyclerView and Task/ThreadPoolExecutor .
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CountryListLoadTask extends AsyncTask<Void, Void, List<CountryInfo>> {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Listener {
        void onLoadComplete(List<CountryInfo> result);
    }

    private final Listener mListener;

    List<String> whitelistedCountryCodes;
    List<String> blacklistedCountryCodes;

    public CountryListLoadTask(Listener listener,
                               List<String> whitelistedCountryCodes,
                               List<String> blacklistedCountryCodes) {
        mListener = listener;
        this.whitelistedCountryCodes = whitelistedCountryCodes;
        this.blacklistedCountryCodes = blacklistedCountryCodes;
    }

    @Override
    protected List<CountryInfo> doInBackground(Void... params) {
        final Map<String, Integer> countryInfoMap = setupEntireCountrySet();
        List<CountryInfo> countryInfoList = getAvailableCountryCodes(countryInfoMap);
        Collections.sort(countryInfoList);
        return countryInfoList;
    }

    public List<CountryInfo> getAvailableCountryCodes(Map<String, Integer> countryInfoMap) {
        if (whitelistedCountryCodes == null && blacklistedCountryCodes == null) {
            whitelistedCountryCodes = new ArrayList<>(countryInfoMap.keySet());
        }

        List<CountryInfo> countryInfoList = new ArrayList<>();

        Set<String> excludedCountries = new HashSet<>();
        if (whitelistedCountryCodes == null) {
            excludedCountries.addAll(blacklistedCountryCodes);
        } else {
            excludedCountries.addAll(countryInfoMap.keySet());
            excludedCountries.removeAll(whitelistedCountryCodes);
        }

        for (String countryCode : countryInfoMap.keySet()) {
            if (!excludedCountries.contains(countryCode)) {
                countryInfoList.add(new CountryInfo(new Locale("", countryCode),
                        countryInfoMap.get(countryCode)));
            }
        }

        return countryInfoList;
    }

    @Override
    public void onPostExecute(List<CountryInfo> result) {
        if (mListener != null) {
            mListener.onLoadComplete(result);
        }
    }

    public Map<String, Integer> setupEntireCountrySet() {
        Map<String, Integer> countryInfoMap = new HashMap<>();
        countryInfoMap.put("AF", 93);
        countryInfoMap.put("AX", 358);
        countryInfoMap.put("AL", 355);
        countryInfoMap.put("DZ", 213);
        countryInfoMap.put("AS", 1);
        countryInfoMap.put("AD", 376);
        countryInfoMap.put("AO", 244);
        countryInfoMap.put("AI", 1);
        countryInfoMap.put("AG", 1);
        countryInfoMap.put("AR", 54);
        countryInfoMap.put("AM", 374);
        countryInfoMap.put("AW", 297);
        countryInfoMap.put("AC", 247);
        countryInfoMap.put("AU", 61);
        countryInfoMap.put("AT", 43);
        countryInfoMap.put("AZ", 994);
        countryInfoMap.put("BS", 1);
        countryInfoMap.put("BH", 973);
        countryInfoMap.put("BD", 880);
        countryInfoMap.put("BB", 1);
        countryInfoMap.put("BY", 375);
        countryInfoMap.put("BE", 32);
        countryInfoMap.put("BZ", 501);
        countryInfoMap.put("BJ", 229);
        countryInfoMap.put("BM", 1);
        countryInfoMap.put("BT", 975);
        countryInfoMap.put("BO", 591);
        countryInfoMap.put("BA", 387);
        countryInfoMap.put("BW", 267);
        countryInfoMap.put("BR", 55);
        countryInfoMap.put("IO", 246);
        countryInfoMap.put("VG", 1);
        countryInfoMap.put("BN", 673);
        countryInfoMap.put("BG", 359);
        countryInfoMap.put("BF", 226);
        countryInfoMap.put("BI", 257);
        countryInfoMap.put("KH", 855);
        countryInfoMap.put("CM", 237);
        countryInfoMap.put("CA", 1);
        countryInfoMap.put("CV", 238);
        countryInfoMap.put("BQ", 599);
        countryInfoMap.put("KY", 1);
        countryInfoMap.put("CF", 236);
        countryInfoMap.put("TD", 235);
        countryInfoMap.put("CL", 56);
        countryInfoMap.put("CN", 86);
        countryInfoMap.put("CX", 61);
        countryInfoMap.put("CC", 61);
        countryInfoMap.put("CO", 57);
        countryInfoMap.put("KM", 269);
        countryInfoMap.put("CD", 243);
        countryInfoMap.put("CG", 242);
        countryInfoMap.put("CK", 682);
        countryInfoMap.put("CR", 506);
        countryInfoMap.put("CI", 225);
        countryInfoMap.put("HR", 385);
        countryInfoMap.put("CU", 53);
        countryInfoMap.put("CW", 599);
        countryInfoMap.put("CY", 357);
        countryInfoMap.put("CZ", 420);
        countryInfoMap.put("DK", 45);
        countryInfoMap.put("DJ", 253);
        countryInfoMap.put("DM", 1);
        countryInfoMap.put("DO", 1);
        countryInfoMap.put("TL", 670);
        countryInfoMap.put("EC", 593);
        countryInfoMap.put("EG", 20);
        countryInfoMap.put("SV", 503);
        countryInfoMap.put("GQ", 240);
        countryInfoMap.put("ER", 291);
        countryInfoMap.put("EE", 372);
        countryInfoMap.put("ET", 251);
        countryInfoMap.put("FK", 500);
        countryInfoMap.put("FO", 298);
        countryInfoMap.put("FJ", 679);
        countryInfoMap.put("FI", 358);
        countryInfoMap.put("FR", 33);
        countryInfoMap.put("GF", 594);
        countryInfoMap.put("PF", 689);
        countryInfoMap.put("GA", 241);
        countryInfoMap.put("GM", 220);
        countryInfoMap.put("GE", 995);
        countryInfoMap.put("DE", 49);
        countryInfoMap.put("GH", 233);
        countryInfoMap.put("GI", 350);
        countryInfoMap.put("GR", 30);
        countryInfoMap.put("GL", 299);
        countryInfoMap.put("GD", 1);
        countryInfoMap.put("GP", 590);
        countryInfoMap.put("GU", 1);
        countryInfoMap.put("GT", 502);
        countryInfoMap.put("GG", 44);
        countryInfoMap.put("GN", 224);
        countryInfoMap.put("GW", 245);
        countryInfoMap.put("GY", 592);
        countryInfoMap.put("HT", 509);
        countryInfoMap.put("HM", 672);
        countryInfoMap.put("HN", 504);
        countryInfoMap.put("HK", 852);
        countryInfoMap.put("HU", 36);
        countryInfoMap.put("IS", 354);
        countryInfoMap.put("IN", 91);
        countryInfoMap.put("ID", 62);
        countryInfoMap.put("IR", 98);
        countryInfoMap.put("IQ", 964);
        countryInfoMap.put("IE", 353);
        countryInfoMap.put("IM", 44);
        countryInfoMap.put("IL", 972);
        countryInfoMap.put("IT", 39);
        countryInfoMap.put("JM", 1);
        countryInfoMap.put("JP", 81);
        countryInfoMap.put("JE", 44);
        countryInfoMap.put("JO", 962);
        countryInfoMap.put("KZ", 7);
        countryInfoMap.put("KE", 254);
        countryInfoMap.put("KI", 686);
        countryInfoMap.put("XK", 381);
        countryInfoMap.put("KW", 965);
        countryInfoMap.put("KG", 996);
        countryInfoMap.put("LA", 856);
        countryInfoMap.put("LV", 371);
        countryInfoMap.put("LB", 961);
        countryInfoMap.put("LS", 266);
        countryInfoMap.put("LR", 231);
        countryInfoMap.put("LY", 218);
        countryInfoMap.put("LI", 423);
        countryInfoMap.put("LT", 370);
        countryInfoMap.put("LU", 352);
        countryInfoMap.put("MO", 853);
        countryInfoMap.put("MK", 389);
        countryInfoMap.put("MG", 261);
        countryInfoMap.put("MW", 265);
        countryInfoMap.put("MY", 60);
        countryInfoMap.put("MV", 960);
        countryInfoMap.put("ML", 223);
        countryInfoMap.put("MT", 356);
        countryInfoMap.put("MH", 692);
        countryInfoMap.put("MQ", 596);
        countryInfoMap.put("MR", 222);
        countryInfoMap.put("MU", 230);
        countryInfoMap.put("YT", 262);
        countryInfoMap.put("MX", 52);
        countryInfoMap.put("FM", 691);
        countryInfoMap.put("MD", 373);
        countryInfoMap.put("MC", 377);
        countryInfoMap.put("MN", 976);
        countryInfoMap.put("ME", 382);
        countryInfoMap.put("MS", 1);
        countryInfoMap.put("MA", 212);
        countryInfoMap.put("MZ", 258);
        countryInfoMap.put("MM", 95);
        countryInfoMap.put("NA", 264);
        countryInfoMap.put("NR", 674);
        countryInfoMap.put("NP", 977);
        countryInfoMap.put("NL", 31);
        countryInfoMap.put("NC", 687);
        countryInfoMap.put("NZ", 64);
        countryInfoMap.put("NI", 505);
        countryInfoMap.put("NE", 227);
        countryInfoMap.put("NG", 234);
        countryInfoMap.put("NU", 683);
        countryInfoMap.put("NF", 672);
        countryInfoMap.put("KP", 850);
        countryInfoMap.put("MP", 1);
        countryInfoMap.put("NO", 47);
        countryInfoMap.put("OM", 968);
        countryInfoMap.put("PK", 92);
        countryInfoMap.put("PW", 680);
        countryInfoMap.put("PS", 970);
        countryInfoMap.put("PA", 507);
        countryInfoMap.put("PG", 675);
        countryInfoMap.put("PY", 595);
        countryInfoMap.put("PE", 51);
        countryInfoMap.put("PH", 63);
        countryInfoMap.put("PL", 48);
        countryInfoMap.put("PT", 351);
        countryInfoMap.put("PR", 1);
        countryInfoMap.put("QA", 974);
        countryInfoMap.put("RE", 262);
        countryInfoMap.put("RO", 40);
        countryInfoMap.put("RU", 7);
        countryInfoMap.put("RW", 250);
        countryInfoMap.put("BL", 590);
        countryInfoMap.put("SH", 290);
        countryInfoMap.put("KN", 1);
        countryInfoMap.put("LC", 1);
        countryInfoMap.put("MF", 590);
        countryInfoMap.put("PM", 508);
        countryInfoMap.put("VC", 1);
        countryInfoMap.put("WS", 685);
        countryInfoMap.put("SM", 378);
        countryInfoMap.put("ST", 239);
        countryInfoMap.put("SA", 966);
        countryInfoMap.put("SN", 221);
        countryInfoMap.put("RS", 381);
        countryInfoMap.put("SC", 248);
        countryInfoMap.put("SL", 232);
        countryInfoMap.put("SG", 65);
        countryInfoMap.put("SX", 1);
        countryInfoMap.put("SK", 421);
        countryInfoMap.put("SI", 386);
        countryInfoMap.put("SB", 677);
        countryInfoMap.put("SO", 252);
        countryInfoMap.put("ZA", 27);
        countryInfoMap.put("GS", 500);
        countryInfoMap.put("KR", 82);
        countryInfoMap.put("SS", 211);
        countryInfoMap.put("ES", 34);
        countryInfoMap.put("LK", 94);
        countryInfoMap.put("SD", 249);
        countryInfoMap.put("SR", 597);
        countryInfoMap.put("SJ", 47);
        countryInfoMap.put("SZ", 268);
        countryInfoMap.put("SE", 46);
        countryInfoMap.put("CH", 41);
        countryInfoMap.put("SY", 963);
        countryInfoMap.put("TW", 886);
        countryInfoMap.put("TJ", 992);
        countryInfoMap.put("TZ", 255);
        countryInfoMap.put("TH", 66);
        countryInfoMap.put("TG", 228);
        countryInfoMap.put("TK", 690);
        countryInfoMap.put("TO", 676);
        countryInfoMap.put("TT", 1);
        countryInfoMap.put("TN", 216);
        countryInfoMap.put("TR", 90);
        countryInfoMap.put("TM", 993);
        countryInfoMap.put("TC", 1);
        countryInfoMap.put("TV", 688);
        countryInfoMap.put("VI", 1);
        countryInfoMap.put("UG", 256);
        countryInfoMap.put("UA", 380);
        countryInfoMap.put("AE", 971);
        countryInfoMap.put("GB", 44);
        countryInfoMap.put("US", 1);
        countryInfoMap.put("UY", 598);
        countryInfoMap.put("UZ", 998);
        countryInfoMap.put("VU", 678);
        countryInfoMap.put("VA", 379);
        countryInfoMap.put("VE", 58);
        countryInfoMap.put("VN", 84);
        countryInfoMap.put("WF", 681);
        countryInfoMap.put("EH", 212);
        countryInfoMap.put("YE", 967);
        countryInfoMap.put("ZM", 260);
        countryInfoMap.put("ZW", 263);
        return countryInfoMap;
    }
}
