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

import com.firebase.ui.auth.data.client.CountryListLoadTask;
import com.firebase.ui.auth.data.model.CountryInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class CountryListLoadTaskTests {
    private static final ArrayList<CountryInfo> COUNTRY_LIST = new ArrayList<>();

    static {
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AF"), 93));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AX"), 358));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AL"), 355));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "DZ"), 213));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AS"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AD"), 376));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AO"), 244));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AI"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AG"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AR"), 54));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AM"), 374));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AW"), 297));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AC"), 247));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AU"), 61));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AT"), 43));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AZ"), 994));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BS"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BH"), 973));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BD"), 880));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BB"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BY"), 375));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BE"), 32));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BZ"), 501));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BJ"), 229));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BM"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BT"), 975));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BO"), 591));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BA"), 387));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BW"), 267));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BR"), 55));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IO"), 246));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VG"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BN"), 673));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BG"), 359));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BF"), 226));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BI"), 257));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KH"), 855));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CM"), 237));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CA"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CV"), 238));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BQ"), 599));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KY"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CF"), 236));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TD"), 235));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CL"), 56));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CN"), 86));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CX"), 61));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CC"), 61));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CO"), 57));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KM"), 269));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CD"), 243));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CG"), 242));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CK"), 682));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CR"), 506));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CI"), 225));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "HR"), 385));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CU"), 53));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CW"), 599));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CY"), 357));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CZ"), 420));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "DK"), 45));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "DJ"), 253));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "DM"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "DO"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TL"), 670));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "EC"), 593));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "EG"), 20));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SV"), 503));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GQ"), 240));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ER"), 291));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "EE"), 372));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ET"), 251));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "FK"), 500));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "FO"), 298));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "FJ"), 679));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "FI"), 358));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "FR"), 33));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GF"), 594));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PF"), 689));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GA"), 241));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GM"), 220));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GE"), 995));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "DE"), 49));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GH"), 233));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GI"), 350));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GR"), 30));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GL"), 299));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GD"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GP"), 590));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GU"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GT"), 502));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GG"), 44));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GN"), 224));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GW"), 245));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GY"), 592));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "HT"), 509));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "HM"), 672));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "HN"), 504));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "HK"), 852));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "HU"), 36));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IS"), 354));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IN"), 91));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ID"), 62));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IR"), 98));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IQ"), 964));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IE"), 353));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IM"), 44));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IL"), 972));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "IT"), 39));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "JM"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "JP"), 81));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "JE"), 44));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "JO"), 962));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KZ"), 7));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KE"), 254));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KI"), 686));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "XK"), 381));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KW"), 965));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KG"), 996));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LA"), 856));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LV"), 371));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LB"), 961));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LS"), 266));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LR"), 231));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LY"), 218));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LI"), 423));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LT"), 370));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LU"), 352));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MO"), 853));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MK"), 389));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MG"), 261));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MW"), 265));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MY"), 60));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MV"), 960));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ML"), 223));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MT"), 356));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MH"), 692));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MQ"), 596));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MR"), 222));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MU"), 230));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "YT"), 262));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MX"), 52));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "FM"), 691));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MD"), 373));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MC"), 377));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MN"), 976));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ME"), 382));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MS"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MA"), 212));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MZ"), 258));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MM"), 95));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NA"), 264));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NR"), 674));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NP"), 977));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NL"), 31));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NC"), 687));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NZ"), 64));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NI"), 505));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NE"), 227));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NG"), 234));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NU"), 683));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NF"), 672));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KP"), 850));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MP"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "NO"), 47));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "OM"), 968));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PK"), 92));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PW"), 680));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PS"), 970));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PA"), 507));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PG"), 675));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PY"), 595));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PE"), 51));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PH"), 63));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PL"), 48));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PT"), 351));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PR"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "QA"), 974));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "RE"), 262));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "RO"), 40));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "RU"), 7));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "RW"), 250));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "BL"), 590));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SH"), 290));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KN"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LC"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "MF"), 590));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "PM"), 508));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VC"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "WS"), 685));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SM"), 378));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ST"), 239));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SA"), 966));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SN"), 221));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "RS"), 381));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SC"), 248));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SL"), 232));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SG"), 65));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SX"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SK"), 421));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SI"), 386));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SB"), 677));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SO"), 252));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ZA"), 27));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GS"), 500));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "KR"), 82));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SS"), 211));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ES"), 34));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "LK"), 94));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SD"), 249));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SR"), 597));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SJ"), 47));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SZ"), 268));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SE"), 46));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "CH"), 41));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "SY"), 963));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TW"), 886));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TJ"), 992));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TZ"), 255));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TH"), 66));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TG"), 228));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TK"), 690));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TO"), 676));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TT"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TN"), 216));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TR"), 90));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TM"), 993));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TC"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "TV"), 688));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VI"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "UG"), 256));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "UA"), 380));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "AE"), 971));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "GB"), 44));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "US"), 1));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "UY"), 598));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "UZ"), 998));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VU"), 678));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VA"), 379));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VE"), 58));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "VN"), 84));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "WF"), 681));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "EH"), 212));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "YE"), 967));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ZM"), 260));
        COUNTRY_LIST.add(new CountryInfo(new Locale("", "ZW"), 263));
    }

    private CountryListLoadTask mTask;
    private CountryListLoadTask.Listener mListener;

    @Before
    public void setUp() {
        // Create task and mock dependencies
        mListener = mock(CountryListLoadTask.Listener.class);
        mTask = new CountryListLoadTask(mListener);
    }

    @Test
    public void testExecute() {
        mTask.execute();

        try {
            final List<CountryInfo> result = mTask.get();
            Collections.sort(COUNTRY_LIST);
            assertEquals(COUNTRY_LIST, result);
        } catch (InterruptedException e) {
            fail("Should not throw InterruptedException");
        } catch (ExecutionException e) {
            fail("Should not throw ExecutionException");
        }
    }

    @Test
    public void testOnPostExecute_nullListener() {
        mTask = new CountryListLoadTask(null);
        try {
            mTask.onPostExecute(COUNTRY_LIST);
        } catch (NullPointerException ex) {
            fail("Should not throw NullPointerException");
        }
    }

    @Test
    public void testOnPostExecute() {
        mTask.onPostExecute(COUNTRY_LIST);
        verify(mListener).onLoadComplete(COUNTRY_LIST);
    }
}
