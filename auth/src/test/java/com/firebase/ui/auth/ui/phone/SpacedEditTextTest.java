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

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SpacedEditTextTest {
    SpacedEditText spacedEditText;
    AttributeSet attrs;
    Context context;
    TypedArray array;
    final float spacingProportion = 1.1f;

    @Before
    public void setUp() throws Exception {
        attrs = mock(AttributeSet.class);
        context = mock(Context.class);
        array = mock(TypedArray.class);

        when(array.getFloat(R.styleable.SpacedEditText_spacingProportion, 1))
                .thenReturn(spacingProportion);
        when(context.obtainStyledAttributes(attrs, R.styleable.SpacedEditText)).thenReturn(array);
        spacedEditText = new SpacedEditText(RuntimeEnvironment.application, attrs);
        spacedEditText.initAttrs(context, attrs);
    }

    @Test
    public void testSpacedEditText_setTextEmpty() throws Exception {
        spacedEditText.setText("");
        testSpacing("", "", spacedEditText);
    }

    @Test
    public void testSpacedEditText_setTextNonEmpty() throws Exception {
        spacedEditText.setText("123456");
        testSpacing("1 2 3 4 5 6", "123456", spacedEditText);
    }

    @Test
    public void testSpacedEditText_setTextWithOneCharacter() throws Exception {
        spacedEditText.setText("1");
        testSpacing("1", "1", spacedEditText);
    }

    @Test
    public void testSpacedEditText_setTextWithExistingSpaces() throws Exception {
        spacedEditText.setText("1 2 3");
        testSpacing("1   2   3", "1 2 3", spacedEditText);
    }

    @Test
    public void testSpacedEditText_noSetText() throws Exception {
        testSpacing("", "", spacedEditText);
    }

    @Test
    public void testSpacedEditText_setLeadingSelection() throws Exception {
        spacedEditText.setText("123456");
        spacedEditText.setSelection(0);
        assertEquals(0, spacedEditText.getSelectionStart());
    }

    @Test
    public void testSpacedEditText_setInnerSelection() throws Exception {
        spacedEditText.setText("123456");
        spacedEditText.setSelection(3);
        assertEquals(5, spacedEditText.getSelectionStart());
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
            assertEquals(spacingProportion, span.getScaleX());

            final int spanStart = editable.getSpanStart(span);
            final int spanEnd = editable.getSpanEnd(span);

            assertEquals(" ", editable.toString().substring(spanStart, spanEnd));
        }
    }

}
