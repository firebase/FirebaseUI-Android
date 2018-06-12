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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CountryListSpinner extends AppCompatEditText implements View.OnClickListener {

    private static final String KEY_SUPER_STATE = "KEY_SUPER_STATE";
    private static final String KEY_COUNTRY_INFO = "KEY_COUNTRY_INFO";

    private final String mTextFormat;
    private final DialogPopup mDialogPopup;
    private final CountryListAdapter mCountryListAdapter;
    private OnClickListener mListener;
    private String mSelectedCountryName;
    private CountryInfo mSelectedCountryInfo;

    private Set<String> mWhitelistedCountryIsos;
    private Set<String> mBlacklistedCountryIsos;

    public CountryListSpinner(Context context) {
        this(context, null, android.R.attr.spinnerStyle);
    }

    public CountryListSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.spinnerStyle);
    }

    public CountryListSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnClickListener(this);

        mCountryListAdapter = new CountryListAdapter(getContext());
        mDialogPopup = new DialogPopup(mCountryListAdapter);
        mTextFormat = "%1$s  +%2$d";
        mSelectedCountryName = "";
    }

    public void init(Bundle params) {
        if (params != null) {
            List<CountryInfo> countries = getCountriesToDisplayInSpinner(params);
            setCountriesToDisplay(countries);
            setDefaultCountryForSpinner(countries);
        }
    }

    private List<CountryInfo> getCountriesToDisplayInSpinner(Bundle params) {
        initCountrySpinnerIsosFromParams(params);

        Map<String, Integer> countryInfoMap = PhoneNumberUtils.getImmutableCountryIsoMap();
        // We consider all countries to be whitelisted if there are no whitelisted
        // or blacklisted countries given as input.
        if (mWhitelistedCountryIsos == null && mBlacklistedCountryIsos == null) {
            this.mWhitelistedCountryIsos = new HashSet<>(countryInfoMap.keySet());
        }

        List<CountryInfo> countryInfoList = new ArrayList<>();

        // At this point either mWhitelistedCountryIsos or mBlacklistedCountryIsos is null.
        // We assume no countries are to be excluded. Here, we correct this assumption based on the
        // contents of either lists.
        Set<String> excludedCountries = new HashSet<>();
        if (mWhitelistedCountryIsos == null) {
            // Exclude all countries in the mBlacklistedCountryIsos list.
            excludedCountries.addAll(mBlacklistedCountryIsos);
        } else {
            // Exclude all countries that are not present in the mWhitelistedCountryIsos list.
            excludedCountries.addAll(countryInfoMap.keySet());
            excludedCountries.removeAll(mWhitelistedCountryIsos);
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
        List<String> whitelistedCountries =
                params.getStringArrayList(ExtraConstants.WHITELISTED_COUNTRIES);
        List<String> blacklistedCountries =
                params.getStringArrayList(ExtraConstants.BLACKLISTED_COUNTRIES);

        if (whitelistedCountries != null) {
            mWhitelistedCountryIsos = convertCodesToIsos(whitelistedCountries);
        } else if (blacklistedCountries != null) {
            mBlacklistedCountryIsos = convertCodesToIsos(blacklistedCountries);
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
        mCountryListAdapter.setData(countries);
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
        return ((mWhitelistedCountryIsos == null && mBlacklistedCountryIsos == null)
                || (mWhitelistedCountryIsos != null && mWhitelistedCountryIsos.contains(iso))
                || (mBlacklistedCountryIsos != null && !mBlacklistedCountryIsos.contains(iso)));
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
        setText(String.format(mTextFormat, CountryInfo.localeToEmoji(locale), countryCode));
        mSelectedCountryInfo = new CountryInfo(locale, countryCode);
    }

    public void setSelectedForCountry(final Locale locale, String countryCode) {
        if (isValidIso(locale.getCountry())) {
            final String countryName = locale.getDisplayName();
            if (!TextUtils.isEmpty(countryName) && !TextUtils.isEmpty(countryCode)) {
                mSelectedCountryName = countryName;
                setSelectedForCountry(Integer.parseInt(countryCode), locale);
            }
        }
    }

    public CountryInfo getSelectedCountryInfo() {
        return mSelectedCountryInfo;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mDialogPopup.isShowing()) {
            mDialogPopup.dismiss();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mListener = l;
    }

    @Override
    public void onClick(View view) {
        mDialogPopup.show(mCountryListAdapter.getPositionForCountry(mSelectedCountryName));
        hideKeyboard(getContext(), this);
        executeUserClickListener(view);
    }

    private void executeUserClickListener(View view) {
        if (mListener != null) {
            mListener.onClick(view);
        }
    }

    public class DialogPopup implements DialogInterface.OnClickListener {
        //Delay for postDelayed to set selection without showing the scroll animation
        private static final long DELAY_MILLIS = 10L;
        private final CountryListAdapter listAdapter;
        private AlertDialog dialog;

        DialogPopup(CountryListAdapter adapter) {
            listAdapter = adapter;
        }

        public void dismiss() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }

        public boolean isShowing() {
            return dialog != null && dialog.isShowing();
        }

        public void show(final int selected) {
            if (listAdapter == null) {
                return;
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            dialog = builder.setSingleChoiceItems(listAdapter, 0, this).create();
            dialog.setCanceledOnTouchOutside(true);
            final ListView listView = dialog.getListView();
            listView.setFastScrollEnabled(true);
            listView.setScrollbarFadingEnabled(false);
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(selected);
                }
            }, DELAY_MILLIS);
            dialog.show();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final CountryInfo countryInfo = listAdapter.getItem(which);
            mSelectedCountryName = countryInfo.getLocale().getDisplayCountry();
            setSelectedForCountry(countryInfo.getCountryCode(), countryInfo.getLocale());
            dismiss();
        }
    }
}
