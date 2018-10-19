package com.firebase.ui.auth.util.ui;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

public class TextHelper {

    public static void boldAllOccurencesOfText(@NonNull SpannableStringBuilder builder,
                                               @NonNull String text,
                                               @NonNull String textToBold) {
        int fromIndex = 0;
        while (fromIndex < text.length()) {
            int start = text.indexOf(textToBold, fromIndex);
            int end = start + textToBold.length();
            if (start == -1 || end >= text.length()) {
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
