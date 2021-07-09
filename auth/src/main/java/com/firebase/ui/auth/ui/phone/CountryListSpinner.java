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
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

public final class CountryListSpinner extends TextInputEditText implements View.OnClickListener {

    private static final String KEY_SUPER_STATE = "KEY_SUPER_STATE";
    private static final String KEY_COUNTRY_INFO = "KEY_COUNTRY_INFO";

    private final ArrayAdapter<CountryInfo> mCountryListAdapter;
    private View.OnClickListener mListener;
    private CountryInfo mSelectedCountryInfo;

    private ListPopupWindow mListPopupWindow;

    private Set<String> mAllowedCountryIsos = new HashSet<>();
    private Set<String> mBlockedCountryIsos = new HashSet<>();

    public CountryListSpinner(Context context) {
        this(context, null);
    }

    public CountryListSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public CountryListSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnClickListener(this);

        mCountryListAdapter = new ArrayAdapter<>(getContext(),
                R.layout.fui_dgts_country_row,
                android.R.id.text1);
        mListPopupWindow = new ListPopupWindow(context, null, R.attr.listPopupWindowStyle);
        mListPopupWindow.setModal(true);

        // Prevent the keyboard from showing
        setInputType(EditorInfo.TYPE_NULL);

        mListPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CountryInfo info = mCountryListAdapter.getItem(position);
                if (info != null) {
                    setText(info.toShortString());
                }

                onUnfocus();
            }
        });
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            onFocus();
        } else {
            onUnfocus();
        }
    }

    private void onFocus() {
        hideKeyboard(getContext(), this);
        mListPopupWindow.show();
    }

    private void onUnfocus() {
        mListPopupWindow.dismiss();
    }

    public void init(Bundle params, View anchorView) {
        if (params != null) {
            List<CountryInfo> countries = getCountriesToDisplayInSpinner(params);
            setCountriesToDisplay(countries);
            setDefaultCountryForSpinner(countries);

            mListPopupWindow.setAnchorView(anchorView);
            mListPopupWindow.setAdapter(mCountryListAdapter);
        }
    }

    private List<CountryInfo> getCountriesToDisplayInSpinner(Bundle params) {
        initCountrySpinnerIsosFromParams(params);
        Map<String, Integer> countryInfoMap = PhoneNumberUtils.getImmutableCountryIsoMap();
        
        // We consider all countries to be allowed if there are no allowed
        // or blocked countries given as input.
        if (mAllowedCountryIsos.isEmpty() && mBlockedCountryIsos.isEmpty()) {
            this.mAllowedCountryIsos = new HashSet<>(countryInfoMap.keySet());
        }

        List<CountryInfo> countryInfoList = new ArrayList<>();

        // At this point either mAllowedCountryIsos or mBlockedCountryIsos is null.
        // We assume no countries are to be excluded. Here, we correct this assumption based on the
        // contents of either lists.
        Set<String> excludedCountries = new HashSet<>();
        if (!mBlockedCountryIsos.isEmpty()) {
            // Exclude all countries in the mBlockedCountryIsos list.
            excludedCountries.addAll(mBlockedCountryIsos);
        } else {
            // Exclude all countries that are not present in the mAllowedCountryIsos list.
            excludedCountries.addAll(countryInfoMap.keySet());
            excludedCountries.removeAll(mAllowedCountryIsos);
        }

        // Once we know which countries need to be excluded, we loop through the country isos,
        // skipping those that have been excluded.
        for (String countryIso : countryInfoMap.keySet()) {
            if (!excludedCountries.contains(countryIso)) {
                countryInfoList.add(new CountryInfo(new Locale("", countryIso),
                        countryInfoMap.get(countryIso)));
            }
        }
        Collections.sort(countryInfoList);
        return countryInfoList;
    }

    private void initCountrySpinnerIsosFromParams(@NonNull Bundle params) {
        List<String> allowedCountries =
                params.getStringArrayList(ExtraConstants.ALLOWLISTED_COUNTRIES);
        List<String> blockedCountries =
                params.getStringArrayList(ExtraConstants.BLOCKLISTED_COUNTRIES);

        if (allowedCountries != null) {
            mAllowedCountryIsos = convertCodesToIsos(allowedCountries);
        }

        if (blockedCountries != null) {
            mBlockedCountryIsos = convertCodesToIsos(blockedCountries);
        }
    }

    private Set<String> convertCodesToIsos(@NonNull List<String> codes) {
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

    public void setCountriesToDisplay(List<CountryInfo> countries) {
        mCountryListAdapter.addAll(countries);
        mCountryListAdapter.notifyDataSetChanged();
    }

    private void setDefaultCountryForSpinner(List<CountryInfo> countries) {
        CountryInfo countryInfo = PhoneNumberUtils.getCurrentCountryInfo(getContext());
        if (isValidIso(countryInfo.getLocale().getCountry())) {
            setSelectedForCountry(countryInfo.getCountryCode(),
                    countryInfo.getLocale());
        } else if (countries.iterator().hasNext()) {
            countryInfo = countries.iterator().next();
            setSelectedForCountry(countryInfo.getCountryCode(),
                    countryInfo.getLocale());
        }
    }

    public boolean isValidIso(String iso) {
        iso = iso.toUpperCase(Locale.getDefault());
        boolean valid = true;
        if (!mAllowedCountryIsos.isEmpty()) {
            valid = valid && mAllowedCountryIsos.contains(iso);
        }

        if (!mBlockedCountryIsos.isEmpty()) {
            valid = valid && !mBlockedCountryIsos.contains(iso);
        }

        return valid;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_SUPER_STATE, superState);
        bundle.putParcelable(KEY_COUNTRY_INFO, mSelectedCountryInfo);

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }

        Bundle bundle = (Bundle) state;
        Parcelable superState = bundle.getParcelable(KEY_SUPER_STATE);
        mSelectedCountryInfo = bundle.getParcelable(KEY_COUNTRY_INFO);

        super.onRestoreInstanceState(superState);
    }

    private static void hideKeyboard(Context context, View view) {
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void setSelectedForCountry(int countryCode, Locale locale) {
        mSelectedCountryInfo = new CountryInfo(locale, countryCode);
        setText(mSelectedCountryInfo.toShortString());
    }

    public void setSelectedForCountry(final Locale locale, String countryCode) {
        if (isValidIso(locale.getCountry())) {
            final String countryName = locale.getDisplayName();
            if (!TextUtils.isEmpty(countryName) && !TextUtils.isEmpty(countryCode)) {
                setSelectedForCountry(Integer.parseInt(countryCode), locale);
            }
        }
    }

    public CountryInfo getSelectedCountryInfo() {
        return mSelectedCountryInfo;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mListener = l;
    }

    @Override
    public void onClick(View view) {
        hideKeyboard(getContext(), this);
        executeUserClickListener(view);
    }

    private void executeUserClickListener(View view) {
        if (mListener != null) {
            mListener.onClick(view);
        }

        onFocus();
    }
}
