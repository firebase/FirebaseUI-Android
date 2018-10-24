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
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Array adapter used to display a list of countries with section indices.
 */
final class CountryListAdapter extends ArrayAdapter<CountryInfo> implements SectionIndexer {

    // Map from first letter --> position in the list
    private final HashMap<String, Integer> alphaIndex = new LinkedHashMap<>();

    // Map from display name --> position in the list
    private final HashMap<String, Integer> countryPosition = new LinkedHashMap<>();

    private String[] sections;

    public CountryListAdapter(Context context) {
        super(context, R.layout.fui_dgts_country_row, android.R.id.text1);
    }

    // The list of countries should be sorted using locale-sensitive string comparison
    public void setData(List<CountryInfo> countries) {
        // Create index and add entries to adapter
        int index = 0;
        for (CountryInfo countryInfo : countries) {
            final String key = countryInfo.getLocale()
                    .getDisplayCountry()
                    .substring(0, 1)
                    .toUpperCase(Locale.getDefault());

            if (!alphaIndex.containsKey(key)) {
                alphaIndex.put(key, index);
            }
            countryPosition.put(countryInfo.getLocale().getDisplayCountry(), index);

            index++;
            add(countryInfo);
        }

        sections = new String[alphaIndex.size()];
        alphaIndex.keySet().toArray(sections);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return countryPosition.size();
    }

    @Override
    public Object[] getSections() {
        return sections;
    }

    @Override
    public int getPositionForSection(int index) {
        if (sections == null) {
            return 0;
        }

        // Check index bounds
        if (index <= 0) {
            return 0;
        }
        if (index >= sections.length) {
            index = sections.length - 1;
        }

        // Return the position
        return alphaIndex.get(sections[index]);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (sections == null) {
            return 0;
        }

        for (int i = 0; i < sections.length; i++) {
            if (getPositionForSection(i) > position) {
                return i - 1;
            }
        }

        return 0;
    }

    public int getPositionForCountry(String country) {
        final Integer position = countryPosition.get(country);
        return position == null ? 0 : position;
    }
}
