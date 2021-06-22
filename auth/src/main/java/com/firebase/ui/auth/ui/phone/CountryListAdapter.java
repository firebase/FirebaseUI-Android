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

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Array adapter used to display a list of countries with section indices.
 */
final class CountryListAdapter extends ArrayAdapter<CountryInfo> {

    public CountryListAdapter(Context context) {
        super(context, R.layout.fui_dgts_country_row, android.R.id.text1);
    }

    public void setData(List<CountryInfo> countries) {
        // The list of countries should be sorted using locale-sensitive string comparison
        Collections.sort(countries, new Comparator<CountryInfo>() {
            @Override
            public int compare(CountryInfo o1, CountryInfo o2) {
                String name1 = o1.getLocale().getDisplayCountry();
                String name2 = o2.getLocale().getDisplayCountry();

                return name1.compareTo(name2);
            }
        });

        this.addAll(countries);
        notifyDataSetChanged();
    }
}
