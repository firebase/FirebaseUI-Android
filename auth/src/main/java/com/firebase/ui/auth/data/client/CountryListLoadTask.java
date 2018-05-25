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
import com.firebase.ui.auth.util.data.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.Collections;
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

    List<String> whitelistedCountryIsos;
    List<String> blacklistedCountryIsos;

    public CountryListLoadTask(Listener listener,
                               List<String> whitelistedCountryIsos,
                               List<String> blacklistedCountryIsos) {
        mListener = listener;
        this.whitelistedCountryIsos = whitelistedCountryIsos;
        this.blacklistedCountryIsos = blacklistedCountryIsos;
    }

    @Override
    protected List<CountryInfo> doInBackground(Void... params) {
        List<CountryInfo> countryInfoList = getAvailableCountryIsos();
        Collections.sort(countryInfoList);
        return countryInfoList;
    }

    public List<CountryInfo> getAvailableCountryIsos() {
        Map<String, Integer> countryInfoMap = PhoneNumberUtils.getImmutableCountryIsoMap();
        if (whitelistedCountryIsos == null && blacklistedCountryIsos == null) {
            whitelistedCountryIsos = new ArrayList<>(countryInfoMap.keySet());
        }

        List<CountryInfo> countryInfoList = new ArrayList<>();

        Set<String> excludedCountries = new HashSet<>();
        if (whitelistedCountryIsos == null) {
            excludedCountries.addAll(blacklistedCountryIsos);
        } else {
            excludedCountries.addAll(countryInfoMap.keySet());
            excludedCountries.removeAll(whitelistedCountryIsos);
        }

        for (String countryIso : countryInfoMap.keySet()) {
            if (!excludedCountries.contains(countryIso)) {
                countryInfoList.add(new CountryInfo(new Locale("", countryIso),
                        countryInfoMap.get(countryIso)));
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
}
