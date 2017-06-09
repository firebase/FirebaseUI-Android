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
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.firebase.ui.auth.ui.email.PreambleHandler;

/**
 * Text view to display terms of service before completing signup.
 * The view helps display TOS linking to the provided custom URI.
 * It handles the styling of the link and opens the uri in a CustomTabs on click.
 */
public class TermsTextView extends AppCompatTextView {
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
     * @param params     FlowParameters containing terms URLs.
     * @param buttonText for the button that represents the "action" described in the terms.
     */
    public void showTerms(FlowParameters params, @StringRes int buttonText) {
        PreambleHandler handler = new PreambleHandler(getContext(), params, buttonText);
        handler.setPreamble(this);
    }
}
