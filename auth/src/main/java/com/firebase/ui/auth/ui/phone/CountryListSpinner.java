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
import android.os.AsyncTask;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import com.firebase.ui.auth.data.client.CountryListLoadTask;
import com.firebase.ui.auth.data.model.CountryInfo;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;

import java.util.List;
import java.util.Locale;

public final class CountryListSpinner extends AppCompatEditText implements
        View.OnClickListener, CountryListLoadTask.Listener {
    private final String mTextFormat;
    private final DialogPopup mDialogPopup;
    private final CountryListAdapter mCountryListAdapter;
    private OnClickListener mListener;
    private String mSelectedCountryName;

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
        CountryInfo countryInfo = PhoneNumberUtils.getCurrentCountryInfo(getContext());
        setSpinnerText(countryInfo.getCountryCode(), countryInfo.getLocale());
    }

    private static void hideKeyboard(Context context, View view) {
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setSpinnerText(int countryCode, Locale locale) {
        setText(String.format(mTextFormat, CountryInfo.localeToEmoji(locale), countryCode));
        setTag(new CountryInfo(locale, countryCode));
    }

    public void setSelectedForCountry(final Locale locale, String countryCode) {
        final String countryName = locale.getDisplayName();
        if (!TextUtils.isEmpty(countryName) && !TextUtils.isEmpty(countryCode)) {
            mSelectedCountryName = countryName;
            setSpinnerText(Integer.parseInt(countryCode), locale);
        }
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
        if (mCountryListAdapter.getCount() == 0) {
            loadCountryList();
        } else {
            mDialogPopup.show(mCountryListAdapter.getPositionForCountry(mSelectedCountryName));
        }
        hideKeyboard(getContext(), this);
        executeUserClickListener(view);
    }

    private void loadCountryList() {
        new CountryListLoadTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void executeUserClickListener(View view) {
        if (mListener != null) {
            mListener.onClick(view);
        }
    }

    @Override
    public void onLoadComplete(List<CountryInfo> result) {
        mCountryListAdapter.setData(result);
        mDialogPopup.show(mCountryListAdapter.getPositionForCountry(mSelectedCountryName));
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
            setSpinnerText(countryInfo.getCountryCode(), countryInfo.getLocale());
            dismiss();
        }
    }
}
