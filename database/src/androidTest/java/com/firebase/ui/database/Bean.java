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

package com.firebase.ui.database;

public class Bean {
    private int mNumber;
    private String mText;
    private boolean mBool;

    public Bean() {
        // Needed for Firebase
    }

    public Bean(int number, String text, boolean bool) {
        mNumber = number;
        mText = text;
        mBool = bool;
    }

    public Bean(int index) {
        this(index, "Text " + index, index % 2 == 0);
    }

    public int getNumber() {
        return mNumber;
    }

    public void setNumber(int number) {
        mNumber = number;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public boolean isBool() {
        return mBool;
    }

    public void setBool(boolean bool) {
        mBool = bool;
    }
}
