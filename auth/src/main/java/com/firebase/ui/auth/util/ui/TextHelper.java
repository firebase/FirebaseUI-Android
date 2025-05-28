/*
 * Copyright 2025 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.util.ui;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextHelper {

    public static void boldAllOccurencesOfText(@NonNull SpannableStringBuilder builder,
                                               @NonNull String text,
                                               @NonNull String textToBold) {
        int fromIndex = 0;
        while (fromIndex < text.length()) {
            int start = text.indexOf(textToBold, fromIndex);
            int end = start + textToBold.length();
            if (start == -1 || end > text.length()) {
                break;
            }
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            fromIndex = end + 1;
        }
    }
}
