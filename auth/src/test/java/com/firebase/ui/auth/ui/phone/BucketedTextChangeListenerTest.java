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

import android.widget.EditText;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.testhelpers.CustomRobolectricGradleTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class BucketedTextChangeListenerTest {
    EditText editText;
    BucketedTextChangeListener.ContentChangeCallback contentChangeCallback;
    BucketedTextChangeListener textChangeListener;

    final int expectedLength = 6;
    final String placeHolder = "-";
    final int anyInt = -1;

    @Before
    public void setUp() throws Exception {
        editText = mock(EditText.class);
        contentChangeCallback = mock(BucketedTextChangeListener.ContentChangeCallback.class);
        textChangeListener = new BucketedTextChangeListener(editText, expectedLength,
                placeHolder, contentChangeCallback);
    }

    @Test
    public void testTextChange_empty() {
        textChangeListener.onTextChanged("------", anyInt, anyInt, anyInt);
        testListener(editText, "------", 0, false);
    }

    @Test
    public void testTextChange_atIndex0() {
        textChangeListener.onTextChanged("1------", anyInt, anyInt, anyInt);
        testListener(editText, "1-----", 1, false);
    }

    @Test
    public void testTextChange_atIndex1() {
        textChangeListener.onTextChanged("12-----", anyInt, anyInt, anyInt);
        testListener(editText, "12----", 2, false);
    }

    @Test
    public void testTextChange_atIndex5() {
        textChangeListener.onTextChanged("123456-", anyInt, anyInt, anyInt);
        testListener(editText, "123456", 6, true);
    }

    @Test
    public void testTextChange_exceedingMaxLength() {
        textChangeListener.onTextChanged("1234567", anyInt, anyInt, anyInt);
        testListener(editText, "123456", 6, true);
    }

    @Test
    public void testTextChange_onClear() {
        textChangeListener.onTextChanged("", anyInt, anyInt, anyInt);
        testListener(editText, "------", 0, false);
    }

    @Test
    public void testTextChange_onPartialClear() {
        textChangeListener.onTextChanged("123", anyInt, anyInt, anyInt);
        testListener(editText, "123---", 3, false);
    }

    @Test
    public void testTextChange_onIncorrectInsertion() {
        textChangeListener.onTextChanged("1--3--", anyInt, anyInt, anyInt);
        testListener(editText, "13----", 2, false);
    }

    private void testListener(EditText editText, String expectedText, int expectedSelection,
                              boolean isComplete) {
        final InOrder inOrder = inOrder(editText);
        inOrder.verify(editText).removeTextChangedListener(textChangeListener);
        inOrder.verify(editText).setText(expectedText);
        inOrder.verify(editText).setSelection(expectedSelection);
        inOrder.verify(editText).addTextChangedListener(textChangeListener);
        if (isComplete) {
            verify(contentChangeCallback).whileComplete();
        } else {
            verify(contentChangeCallback).whileIncomplete();
        }
    }
}
