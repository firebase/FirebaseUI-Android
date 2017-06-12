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

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Collections;

/**
 * Listens for changes to a text field that has hyphens and replaces with the characted being
 * typed
 * ------
 * 7-----
 * 76----
 * 764---
 * 7641--
 * 76417-
 * 764176
 */
final class BucketedTextChangeListener implements TextWatcher {
    private final EditText editText;
    private final ContentChangeCallback callback;
    private final String[] postFixes;
    private final String placeHolder;
    private final int expectedContentLength;

    public BucketedTextChangeListener(EditText editText, int expectedContentLength, String
            placeHolder, ContentChangeCallback callback) {
        this.editText = editText;
        this.expectedContentLength = expectedContentLength;
        this.postFixes = generatePostfixArray(placeHolder, expectedContentLength);
        this.callback = callback;
        this.placeHolder = placeHolder;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onTextChanged(CharSequence s, int ignoredParam1, int ignoredParam2, int
            ignoredParam3) {
        // The listener is expected to be used in conjunction with the SpacedEditText.

        // Approach
        // 1) Strip all spaces and hyphens introduced by the SET for aesthetics
        final String numericContents = s.toString().replaceAll(" ", "").replaceAll(placeHolder, "");

        // 2) Trim the content to acceptable length.
        final int enteredContentLength = Math.min(numericContents.length(), expectedContentLength);
        final String enteredContent = numericContents.substring(0, enteredContentLength);

        // 3) Reset the text to be the content + required hyphens. The SET automatically inserts
        // spaces requires for aesthetics. This requires removing and reseting the listener to
        // avoid recursion.
        editText.removeTextChangedListener(this);
        editText.setText(enteredContent + postFixes[expectedContentLength - enteredContentLength]);
        editText.setSelection(enteredContentLength);
        editText.addTextChangedListener(this);

        // 4) Callback listeners waiting on content to be of expected length
        if (enteredContentLength == expectedContentLength && callback != null) {
            callback.whileComplete();
        } else if (callback != null) {
            callback.whileIncomplete();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    /**
     * {@link #generatePostfixArray(CharSequence, int)} with params ("-", 6) returns
     * {"", "-", "--", "---", "----", "-----", "------"}
     *
     * @param repeatableChar
     * @param length
     * @return
     */
    private String[] generatePostfixArray(CharSequence repeatableChar, int length) {
        final String[] ret = new String[length + 1];

        for (int i = 0; i <= length; i++) {
            ret[i] = TextUtils.join("", Collections.nCopies(i, repeatableChar));
        }

        return ret;
    }

    interface ContentChangeCallback {
        /**
         * Idempotent function invoked by the listener when the edit text changes and is of
         * expected length
         */
        void whileComplete();

        /**
         * Idempotent function invoked by the listener when the edit text changes and is not of
         * expected length
         */
        void whileIncomplete();
    }
}
