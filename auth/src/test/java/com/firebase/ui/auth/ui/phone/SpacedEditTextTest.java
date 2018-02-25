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
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.style.ScaleXSpan;
import android.util.AttributeSet;

import com.firebase.ui.auth.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SpacedEditTextTest {
    private static final float SPACING_PROPORTION = 1.1f;

    private SpacedEditText mSpacedEditText;

    @Before
    public void setUp() {
        AttributeSet attrs = mock(AttributeSet.class);
        Context context = mock(Context.class);
        TypedArray array = mock(TypedArray.class);

        when(array.getFloat(R.styleable.SpacedEditText_spacingProportion, 1))
                .thenReturn(SPACING_PROPORTION);
        when(context.obtainStyledAttributes(attrs, R.styleable.SpacedEditText)).thenReturn(array);
        mSpacedEditText = new SpacedEditText(RuntimeEnvironment.application, attrs);
        mSpacedEditText.initAttrs(context, attrs);
    }

    @Test
    public void testSpacedEditText_setTextEmpty() {
        mSpacedEditText.setText("");
        testSpacing("", "", mSpacedEditText);
    }

    @Test
    public void testSpacedEditText_setTextNonEmpty() {
        mSpacedEditText.setText("123456");
        testSpacing("1 2 3 4 5 6", "123456", mSpacedEditText);
    }

    @Test
    public void testSpacedEditText_setTextWithOneCharacter() {
        mSpacedEditText.setText("1");
        testSpacing("1", "1", mSpacedEditText);
    }

    @Test
    public void testSpacedEditText_setTextWithExistingSpaces() {
        mSpacedEditText.setText("1 2 3");
        testSpacing("1   2   3", "1 2 3", mSpacedEditText);
    }

    @Test
    public void testSpacedEditText_noSetText() {
        testSpacing("", "", mSpacedEditText);
    }

    @Test
    public void testSpacedEditText_setLeadingSelection() {
        mSpacedEditText.setText("123456");
        mSpacedEditText.setSelection(0);
        assertEquals(0, mSpacedEditText.getSelectionStart());
    }

    @Test
    public void testSpacedEditText_setInnerSelection() {
        mSpacedEditText.setText("123456");
        mSpacedEditText.setSelection(3);
        assertEquals(5, mSpacedEditText.getSelectionStart());
    }

    /**
     * 1. Tests whether the content is set to the expected value.
     * 2. Tests whether the original content is set to the original value.
     * 3. Tests that the styles applied have the expected proportion
     * 4. Tests that the styles have been applied only on the spaces to preserve fonts appearance.
     */
    private void testSpacing(String expectedSpacedText, String expectedOriginalText,
                             SpacedEditText editText) {
        final Editable editable = editText.getText();
        final ScaleXSpan[] spans = editable.getSpans(0, editText.length(), ScaleXSpan.class);

        assertEquals(expectedSpacedText, editable.toString());
        assertEquals(expectedOriginalText, editText.getUnspacedText().toString());

        for (ScaleXSpan span : spans) {
            assertEquals(SPACING_PROPORTION, span.getScaleX());

            final int spanStart = editable.getSpanStart(span);
            final int spanEnd = editable.getSpanEnd(span);

            assertEquals(" ", editable.toString().substring(spanStart, spanEnd));
        }
    }

}
