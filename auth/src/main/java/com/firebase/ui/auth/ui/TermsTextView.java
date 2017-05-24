/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.StringRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.firebase.ui.auth.R;

/**
 * Text view to display terms of service before completing signup.
 * The view helps display TOS linking to the provided custom URI.
 * It handles the styling of the link and opens the uri in a CustomTabs on click.
 */
public class TermsTextView extends android.support.v7.widget.AppCompatTextView {
    public TermsTextView(Context context) {
        super(context);
    }

    public TermsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TermsTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @param uri        uri to link when the user clicks on Terms of Service
     * @param buttonText for the button that represents the "action" described in the terms.
     */
    public void showTermsForUri(final Uri uri, @StringRes int buttonText) {
        // Format the terms interpolating the action string
        String buttonTextString = getContext().getString(buttonText);
        String preamble = getContext().getString(R.string.create_account_preamble,
                                                 buttonTextString);
        //Apply the link color on the part of the string that links to the TOS upon clicking
        String link = getContext().getString(R.string.terms_of_service);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(preamble + link);
        int start = preamble.length();
        ForegroundColorSpan foregroundColorSpan =
                new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.linkColor));
        spannableStringBuilder.setSpan(foregroundColorSpan, start, start + link.length(), 0);
        setText(spannableStringBuilder);

        // Open in custom tabs on click
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Getting default color
                TypedValue typedValue = new TypedValue();
                getContext().getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
                @ColorInt int color = typedValue.data;

                new CustomTabsIntent.Builder()
                        .setToolbarColor(color)
                        .build()
                        .launchUrl(getContext(), uri);
            }
        });
    }
}
