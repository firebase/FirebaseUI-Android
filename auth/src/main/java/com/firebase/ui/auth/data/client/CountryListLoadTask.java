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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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


    Set<String> whitelistedCountryIsos;
    Set<String> blacklistedCountryIsos;

    public CountryListLoadTask(Listener listener,
                               @Nullable List<String> whitelistedCountries,
                               @Nullable List<String> blacklistedCountries) {
        mListener = listener;

        if (whitelistedCountries != null) {
            this.whitelistedCountryIsos = convertCodesToIsos(new HashSet<>(whitelistedCountries));
        } else if (blacklistedCountries != null) {
            this.blacklistedCountryIsos = convertCodesToIsos(new HashSet<>(blacklistedCountries));
        }
    }

    private Set<String> convertCodesToIsos(@NonNull Set<String> codes) {
        Set<String> isos = new HashSet<>();
        for (String code : codes) {
            if (PhoneNumberUtils.isValid(code)) {
                isos.addAll(PhoneNumberUtils.getCountryIsosFromCountryCode(code));
            } else {
                isos.add(code);
            }
        }
        return isos;
    }

    @Override
    protected List<CountryInfo> doInBackground(Void... params) {
        Map<String, Integer> countryInfoMap = PhoneNumberUtils.getImmutableCountryIsoMap();
        List<CountryInfo> countryInfoList = getAvailableCountryIsos(countryInfoMap);
        Collections.sort(countryInfoList);
        return countryInfoList;
    }

    public List<CountryInfo> getAvailableCountryIsos(Map<String, Integer> countryInfoMap) {
        // We consider all countries to be whitelisted if there are no whitelisted
        // or blacklisted countries given as input.
        if (whitelistedCountryIsos == null && blacklistedCountryIsos == null) {
            this.whitelistedCountryIsos = new HashSet<>(countryInfoMap.keySet());
        }

        List<CountryInfo> countryInfoList = new ArrayList<>();

        // At this point either whitelistedCountryIsos or blacklistedCountryIsos is null.
        // We assume no countries are to be excluded. Here, we correct this assumption based on the
        // contents of either lists.
        Set<String> excludedCountries = new HashSet<>();
        if (whitelistedCountryIsos == null) {
            // Exclude all countries in the blacklistedCountryIsos list.
            excludedCountries.addAll(blacklistedCountryIsos);
        } else {
            // Exclude all countries that are not present in the whitelistedCountryIsos list.
            excludedCountries.addAll(countryInfoMap.keySet());
            excludedCountries.removeAll(whitelistedCountryIsos);
        }

        // Once we know which countries need to be excluded, we loop through the country isos,
        // skipping those that have been excluded.
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
