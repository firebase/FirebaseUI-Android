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
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import java.util.List;
import java.util.Locale;

public final class CountryListSpinner extends AppCompatEditText implements
        View.OnClickListener, CountryListLoadTask.Listener {
    private String textFormat;
    private DialogPopup dialogPopup;
    private CountryListAdapter countryListAdapter;
    private OnClickListener listener;
    private String selectedCountryName;

    public CountryListSpinner(Context context) {
        this(context, null, android.R.attr.spinnerStyle);
    }

    public CountryListSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.spinnerStyle);
    }

    public CountryListSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    @VisibleForTesting
    void setDialogPopup(DialogPopup dialog) {
        this.dialogPopup = dialog;
    }

    private void init() {
        super.setOnClickListener(this);

        countryListAdapter = new CountryListAdapter(getContext());
        dialogPopup = new DialogPopup(countryListAdapter);
        textFormat = "%1$s  +%2$d";
        selectedCountryName = "";
        final CountryInfo countryInfo = PhoneNumberUtils.getCurrentCountryInfo(getContext());
        setSpinnerText(countryInfo.countryCode, countryInfo.locale);
    }

    private void setSpinnerText(int countryCode, Locale locale) {
        setText(String.format(textFormat, CountryInfo.localeToEmoji(locale), countryCode));
        setTag(new CountryInfo(locale, countryCode));
    }

    public void setSelectedForCountry(final Locale locale, String countryCode) {
        final String countryName = locale.getDisplayName();
        if (!TextUtils.isEmpty(countryName) && !TextUtils.isEmpty(countryCode)) {
            selectedCountryName = countryName;
            setSpinnerText(Integer.parseInt(countryCode), locale);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (dialogPopup.isShowing()) {
            dialogPopup.dismiss();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        listener = l;
    }

    @Override
    public void onClick(View view) {
        if (countryListAdapter.getCount() == 0) {
            loadCountryList();
        } else {
            dialogPopup.show(countryListAdapter.getPositionForCountry(selectedCountryName));
        }
        hideKeyboard(getContext(), CountryListSpinner.this);
        executeUserClickListener(view);
    }

    private void loadCountryList() {
        new CountryListLoadTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void executeUserClickListener(View view) {
        if (listener != null) {
            listener.onClick(view);
        }
    }

    @Override
    public void onLoadComplete(List<CountryInfo> result) {
        countryListAdapter.setData(result);
        dialogPopup.show(countryListAdapter.getPositionForCountry(selectedCountryName));
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
            selectedCountryName = countryInfo.locale.getDisplayCountry();
            setSpinnerText(countryInfo.countryCode, countryInfo.locale);
            dismiss();
        }
    }

    public static void hideKeyboard(Context context, View view) {
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context
                .INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
