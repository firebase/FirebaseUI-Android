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

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// We need to move away from ListView and AsyncTask in the future and use (say)
// RecyclerView and Task/ThreadPoolExecutor .
final class CountryListLoadTask extends AsyncTask<Void, Void, List<CountryInfo>> {
    private static final int MAX_COUNTRIES = 291;

    private final Listener listener;

    public CountryListLoadTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected List<CountryInfo> doInBackground(Void... params) {
        final List<CountryInfo> countryInfoList = new ArrayList<>(MAX_COUNTRIES);
        countryInfoList.add(new CountryInfo(new Locale("", "AF"), 93));
        countryInfoList.add(new CountryInfo(new Locale("", "AX"), 358));
        countryInfoList.add(new CountryInfo(new Locale("", "AL"), 355));
        countryInfoList.add(new CountryInfo(new Locale("", "DZ"), 213));
        countryInfoList.add(new CountryInfo(new Locale("", "AS"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "AD"), 376));
        countryInfoList.add(new CountryInfo(new Locale("", "AO"), 244));
        countryInfoList.add(new CountryInfo(new Locale("", "AI"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "AG"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "AR"), 54));
        countryInfoList.add(new CountryInfo(new Locale("", "AM"), 374));
        countryInfoList.add(new CountryInfo(new Locale("", "AW"), 297));
        countryInfoList.add(new CountryInfo(new Locale("", "AC"), 247));
        countryInfoList.add(new CountryInfo(new Locale("", "AU"), 61));
        countryInfoList.add(new CountryInfo(new Locale("", "AT"), 43));
        countryInfoList.add(new CountryInfo(new Locale("", "AZ"), 994));
        countryInfoList.add(new CountryInfo(new Locale("", "BS"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "BH"), 973));
        countryInfoList.add(new CountryInfo(new Locale("", "BD"), 880));
        countryInfoList.add(new CountryInfo(new Locale("", "BB"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "BY"), 375));
        countryInfoList.add(new CountryInfo(new Locale("", "BE"), 32));
        countryInfoList.add(new CountryInfo(new Locale("", "BZ"), 501));
        countryInfoList.add(new CountryInfo(new Locale("", "BJ"), 229));
        countryInfoList.add(new CountryInfo(new Locale("", "BM"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "BT"), 975));
        countryInfoList.add(new CountryInfo(new Locale("", "BO"), 591));
        countryInfoList.add(new CountryInfo(new Locale("", "BA"), 387));
        countryInfoList.add(new CountryInfo(new Locale("", "BW"), 267));
        countryInfoList.add(new CountryInfo(new Locale("", "BR"), 55));
        countryInfoList.add(new CountryInfo(new Locale("", "IO"), 246));
        countryInfoList.add(new CountryInfo(new Locale("", "VG"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "BN"), 673));
        countryInfoList.add(new CountryInfo(new Locale("", "BG"), 359));
        countryInfoList.add(new CountryInfo(new Locale("", "BF"), 226));
        countryInfoList.add(new CountryInfo(new Locale("", "BI"), 257));
        countryInfoList.add(new CountryInfo(new Locale("", "KH"), 855));
        countryInfoList.add(new CountryInfo(new Locale("", "CM"), 237));
        countryInfoList.add(new CountryInfo(new Locale("", "CA"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "CV"), 238));
        countryInfoList.add(new CountryInfo(new Locale("", "BQ"), 599));
        countryInfoList.add(new CountryInfo(new Locale("", "KY"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "CF"), 236));
        countryInfoList.add(new CountryInfo(new Locale("", "TD"), 235));
        countryInfoList.add(new CountryInfo(new Locale("", "CL"), 56));
        countryInfoList.add(new CountryInfo(new Locale("", "CN"), 86));
        countryInfoList.add(new CountryInfo(new Locale("", "CX"), 61));
        countryInfoList.add(new CountryInfo(new Locale("", "CC"), 61));
        countryInfoList.add(new CountryInfo(new Locale("", "CO"), 57));
        countryInfoList.add(new CountryInfo(new Locale("", "KM"), 269));
        countryInfoList.add(new CountryInfo(new Locale("", "CD"), 243));
        countryInfoList.add(new CountryInfo(new Locale("", "CG"), 242));
        countryInfoList.add(new CountryInfo(new Locale("", "CK"), 682));
        countryInfoList.add(new CountryInfo(new Locale("", "CR"), 506));
        countryInfoList.add(new CountryInfo(new Locale("", "CI"), 225));
        countryInfoList.add(new CountryInfo(new Locale("", "HR"), 385));
        countryInfoList.add(new CountryInfo(new Locale("", "CU"), 53));
        countryInfoList.add(new CountryInfo(new Locale("", "CW"), 599));
        countryInfoList.add(new CountryInfo(new Locale("", "CY"), 357));
        countryInfoList.add(new CountryInfo(new Locale("", "CZ"), 420));
        countryInfoList.add(new CountryInfo(new Locale("", "DK"), 45));
        countryInfoList.add(new CountryInfo(new Locale("", "DJ"), 253));
        countryInfoList.add(new CountryInfo(new Locale("", "DM"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "DO"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "TL"), 670));
        countryInfoList.add(new CountryInfo(new Locale("", "EC"), 593));
        countryInfoList.add(new CountryInfo(new Locale("", "EG"), 20));
        countryInfoList.add(new CountryInfo(new Locale("", "SV"), 503));
        countryInfoList.add(new CountryInfo(new Locale("", "GQ"), 240));
        countryInfoList.add(new CountryInfo(new Locale("", "ER"), 291));
        countryInfoList.add(new CountryInfo(new Locale("", "EE"), 372));
        countryInfoList.add(new CountryInfo(new Locale("", "ET"), 251));
        countryInfoList.add(new CountryInfo(new Locale("", "FK"), 500));
        countryInfoList.add(new CountryInfo(new Locale("", "FO"), 298));
        countryInfoList.add(new CountryInfo(new Locale("", "FJ"), 679));
        countryInfoList.add(new CountryInfo(new Locale("", "FI"), 358));
        countryInfoList.add(new CountryInfo(new Locale("", "FR"), 33));
        countryInfoList.add(new CountryInfo(new Locale("", "GF"), 594));
        countryInfoList.add(new CountryInfo(new Locale("", "PF"), 689));
        countryInfoList.add(new CountryInfo(new Locale("", "GA"), 241));
        countryInfoList.add(new CountryInfo(new Locale("", "GM"), 220));
        countryInfoList.add(new CountryInfo(new Locale("", "GE"), 995));
        countryInfoList.add(new CountryInfo(new Locale("", "DE"), 49));
        countryInfoList.add(new CountryInfo(new Locale("", "GH"), 233));
        countryInfoList.add(new CountryInfo(new Locale("", "GI"), 350));
        countryInfoList.add(new CountryInfo(new Locale("", "GR"), 30));
        countryInfoList.add(new CountryInfo(new Locale("", "GL"), 299));
        countryInfoList.add(new CountryInfo(new Locale("", "GD"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "GP"), 590));
        countryInfoList.add(new CountryInfo(new Locale("", "GU"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "GT"), 502));
        countryInfoList.add(new CountryInfo(new Locale("", "GG"), 44));
        countryInfoList.add(new CountryInfo(new Locale("", "GN"), 224));
        countryInfoList.add(new CountryInfo(new Locale("", "GW"), 245));
        countryInfoList.add(new CountryInfo(new Locale("", "GY"), 592));
        countryInfoList.add(new CountryInfo(new Locale("", "HT"), 509));
        countryInfoList.add(new CountryInfo(new Locale("", "HM"), 672));
        countryInfoList.add(new CountryInfo(new Locale("", "HN"), 504));
        countryInfoList.add(new CountryInfo(new Locale("", "HK"), 852));
        countryInfoList.add(new CountryInfo(new Locale("", "HU"), 36));
        countryInfoList.add(new CountryInfo(new Locale("", "IS"), 354));
        countryInfoList.add(new CountryInfo(new Locale("", "IN"), 91));
        countryInfoList.add(new CountryInfo(new Locale("", "ID"), 62));
        countryInfoList.add(new CountryInfo(new Locale("", "IR"), 98));
        countryInfoList.add(new CountryInfo(new Locale("", "IQ"), 964));
        countryInfoList.add(new CountryInfo(new Locale("", "IE"), 353));
        countryInfoList.add(new CountryInfo(new Locale("", "IM"), 44));
        countryInfoList.add(new CountryInfo(new Locale("", "IL"), 972));
        countryInfoList.add(new CountryInfo(new Locale("", "IT"), 39));
        countryInfoList.add(new CountryInfo(new Locale("", "JM"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "JP"), 81));
        countryInfoList.add(new CountryInfo(new Locale("", "JE"), 44));
        countryInfoList.add(new CountryInfo(new Locale("", "JO"), 962));
        countryInfoList.add(new CountryInfo(new Locale("", "KZ"), 7));
        countryInfoList.add(new CountryInfo(new Locale("", "KE"), 254));
        countryInfoList.add(new CountryInfo(new Locale("", "KI"), 686));
        countryInfoList.add(new CountryInfo(new Locale("", "XK"), 381));
        countryInfoList.add(new CountryInfo(new Locale("", "KW"), 965));
        countryInfoList.add(new CountryInfo(new Locale("", "KG"), 996));
        countryInfoList.add(new CountryInfo(new Locale("", "LA"), 856));
        countryInfoList.add(new CountryInfo(new Locale("", "LV"), 371));
        countryInfoList.add(new CountryInfo(new Locale("", "LB"), 961));
        countryInfoList.add(new CountryInfo(new Locale("", "LS"), 266));
        countryInfoList.add(new CountryInfo(new Locale("", "LR"), 231));
        countryInfoList.add(new CountryInfo(new Locale("", "LY"), 218));
        countryInfoList.add(new CountryInfo(new Locale("", "LI"), 423));
        countryInfoList.add(new CountryInfo(new Locale("", "LT"), 370));
        countryInfoList.add(new CountryInfo(new Locale("", "LU"), 352));
        countryInfoList.add(new CountryInfo(new Locale("", "MO"), 853));
        countryInfoList.add(new CountryInfo(new Locale("", "MK"), 389));
        countryInfoList.add(new CountryInfo(new Locale("", "MG"), 261));
        countryInfoList.add(new CountryInfo(new Locale("", "MW"), 265));
        countryInfoList.add(new CountryInfo(new Locale("", "MY"), 60));
        countryInfoList.add(new CountryInfo(new Locale("", "MV"), 960));
        countryInfoList.add(new CountryInfo(new Locale("", "ML"), 223));
        countryInfoList.add(new CountryInfo(new Locale("", "MT"), 356));
        countryInfoList.add(new CountryInfo(new Locale("", "MH"), 692));
        countryInfoList.add(new CountryInfo(new Locale("", "MQ"), 596));
        countryInfoList.add(new CountryInfo(new Locale("", "MR"), 222));
        countryInfoList.add(new CountryInfo(new Locale("", "MU"), 230));
        countryInfoList.add(new CountryInfo(new Locale("", "YT"), 262));
        countryInfoList.add(new CountryInfo(new Locale("", "MX"), 52));
        countryInfoList.add(new CountryInfo(new Locale("", "FM"), 691));
        countryInfoList.add(new CountryInfo(new Locale("", "MD"), 373));
        countryInfoList.add(new CountryInfo(new Locale("", "MC"), 377));
        countryInfoList.add(new CountryInfo(new Locale("", "MN"), 976));
        countryInfoList.add(new CountryInfo(new Locale("", "ME"), 382));
        countryInfoList.add(new CountryInfo(new Locale("", "MS"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "MA"), 212));
        countryInfoList.add(new CountryInfo(new Locale("", "MZ"), 258));
        countryInfoList.add(new CountryInfo(new Locale("", "MM"), 95));
        countryInfoList.add(new CountryInfo(new Locale("", "NA"), 264));
        countryInfoList.add(new CountryInfo(new Locale("", "NR"), 674));
        countryInfoList.add(new CountryInfo(new Locale("", "NP"), 977));
        countryInfoList.add(new CountryInfo(new Locale("", "NL"), 31));
        countryInfoList.add(new CountryInfo(new Locale("", "NC"), 687));
        countryInfoList.add(new CountryInfo(new Locale("", "NZ"), 64));
        countryInfoList.add(new CountryInfo(new Locale("", "NI"), 505));
        countryInfoList.add(new CountryInfo(new Locale("", "NE"), 227));
        countryInfoList.add(new CountryInfo(new Locale("", "NG"), 234));
        countryInfoList.add(new CountryInfo(new Locale("", "NU"), 683));
        countryInfoList.add(new CountryInfo(new Locale("", "NF"), 672));
        countryInfoList.add(new CountryInfo(new Locale("", "KP"), 850));
        countryInfoList.add(new CountryInfo(new Locale("", "MP"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "NO"), 47));
        countryInfoList.add(new CountryInfo(new Locale("", "OM"), 968));
        countryInfoList.add(new CountryInfo(new Locale("", "PK"), 92));
        countryInfoList.add(new CountryInfo(new Locale("", "PW"), 680));
        countryInfoList.add(new CountryInfo(new Locale("", "PS"), 970));
        countryInfoList.add(new CountryInfo(new Locale("", "PA"), 507));
        countryInfoList.add(new CountryInfo(new Locale("", "PG"), 675));
        countryInfoList.add(new CountryInfo(new Locale("", "PY"), 595));
        countryInfoList.add(new CountryInfo(new Locale("", "PE"), 51));
        countryInfoList.add(new CountryInfo(new Locale("", "PH"), 63));
        countryInfoList.add(new CountryInfo(new Locale("", "PL"), 48));
        countryInfoList.add(new CountryInfo(new Locale("", "PT"), 351));
        countryInfoList.add(new CountryInfo(new Locale("", "PR"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "QA"), 974));
        countryInfoList.add(new CountryInfo(new Locale("", "RE"), 262));
        countryInfoList.add(new CountryInfo(new Locale("", "RO"), 40));
        countryInfoList.add(new CountryInfo(new Locale("", "RU"), 7));
        countryInfoList.add(new CountryInfo(new Locale("", "RW"), 250));
        countryInfoList.add(new CountryInfo(new Locale("", "BL"), 590));
        countryInfoList.add(new CountryInfo(new Locale("", "SH"), 290));
        countryInfoList.add(new CountryInfo(new Locale("", "KN"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "LC"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "MF"), 590));
        countryInfoList.add(new CountryInfo(new Locale("", "PM"), 508));
        countryInfoList.add(new CountryInfo(new Locale("", "VC"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "WS"), 685));
        countryInfoList.add(new CountryInfo(new Locale("", "SM"), 378));
        countryInfoList.add(new CountryInfo(new Locale("", "ST"), 239));
        countryInfoList.add(new CountryInfo(new Locale("", "SA"), 966));
        countryInfoList.add(new CountryInfo(new Locale("", "SN"), 221));
        countryInfoList.add(new CountryInfo(new Locale("", "RS"), 381));
        countryInfoList.add(new CountryInfo(new Locale("", "SC"), 248));
        countryInfoList.add(new CountryInfo(new Locale("", "SL"), 232));
        countryInfoList.add(new CountryInfo(new Locale("", "SG"), 65));
        countryInfoList.add(new CountryInfo(new Locale("", "SX"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "SK"), 421));
        countryInfoList.add(new CountryInfo(new Locale("", "SI"), 386));
        countryInfoList.add(new CountryInfo(new Locale("", "SB"), 677));
        countryInfoList.add(new CountryInfo(new Locale("", "SO"), 252));
        countryInfoList.add(new CountryInfo(new Locale("", "ZA"), 27));
        countryInfoList.add(new CountryInfo(new Locale("", "GS"), 500));
        countryInfoList.add(new CountryInfo(new Locale("", "KR"), 82));
        countryInfoList.add(new CountryInfo(new Locale("", "SS"), 211));
        countryInfoList.add(new CountryInfo(new Locale("", "ES"), 34));
        countryInfoList.add(new CountryInfo(new Locale("", "LK"), 94));
        countryInfoList.add(new CountryInfo(new Locale("", "SD"), 249));
        countryInfoList.add(new CountryInfo(new Locale("", "SR"), 597));
        countryInfoList.add(new CountryInfo(new Locale("", "SJ"), 47));
        countryInfoList.add(new CountryInfo(new Locale("", "SZ"), 268));
        countryInfoList.add(new CountryInfo(new Locale("", "SE"), 46));
        countryInfoList.add(new CountryInfo(new Locale("", "CH"), 41));
        countryInfoList.add(new CountryInfo(new Locale("", "SY"), 963));
        countryInfoList.add(new CountryInfo(new Locale("", "TW"), 886));
        countryInfoList.add(new CountryInfo(new Locale("", "TJ"), 992));
        countryInfoList.add(new CountryInfo(new Locale("", "TZ"), 255));
        countryInfoList.add(new CountryInfo(new Locale("", "TH"), 66));
        countryInfoList.add(new CountryInfo(new Locale("", "TG"), 228));
        countryInfoList.add(new CountryInfo(new Locale("", "TK"), 690));
        countryInfoList.add(new CountryInfo(new Locale("", "TO"), 676));
        countryInfoList.add(new CountryInfo(new Locale("", "TT"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "TN"), 216));
        countryInfoList.add(new CountryInfo(new Locale("", "TR"), 90));
        countryInfoList.add(new CountryInfo(new Locale("", "TM"), 993));
        countryInfoList.add(new CountryInfo(new Locale("", "TC"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "TV"), 688));
        countryInfoList.add(new CountryInfo(new Locale("", "VI"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "UG"), 256));
        countryInfoList.add(new CountryInfo(new Locale("", "UA"), 380));
        countryInfoList.add(new CountryInfo(new Locale("", "AE"), 971));
        countryInfoList.add(new CountryInfo(new Locale("", "GB"), 44));
        countryInfoList.add(new CountryInfo(new Locale("", "US"), 1));
        countryInfoList.add(new CountryInfo(new Locale("", "UY"), 598));
        countryInfoList.add(new CountryInfo(new Locale("", "UZ"), 998));
        countryInfoList.add(new CountryInfo(new Locale("", "VU"), 678));
        countryInfoList.add(new CountryInfo(new Locale("", "VA"), 379));
        countryInfoList.add(new CountryInfo(new Locale("", "VE"), 58));
        countryInfoList.add(new CountryInfo(new Locale("", "VN"), 84));
        countryInfoList.add(new CountryInfo(new Locale("", "WF"), 681));
        countryInfoList.add(new CountryInfo(new Locale("", "EH"), 212));
        countryInfoList.add(new CountryInfo(new Locale("", "YE"), 967));
        countryInfoList.add(new CountryInfo(new Locale("", "ZM"), 260));
        countryInfoList.add(new CountryInfo(new Locale("", "ZW"), 263));
        Collections.sort(countryInfoList);
        return countryInfoList;
    }

    @Override
    protected void onPostExecute(List<CountryInfo> result) {
        if (listener != null) {
            listener.onLoadComplete(result);
        }
    }

    public interface Listener {
        void onLoadComplete(List<CountryInfo> result);
    }
}
