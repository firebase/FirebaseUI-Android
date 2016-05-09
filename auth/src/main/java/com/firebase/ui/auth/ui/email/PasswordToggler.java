/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.ui.email;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.firebase.ui.auth.R;

/**
 * Shows and hides the contents of password fields, to improve their usability.
 */
public class PasswordToggler implements ImageView.OnClickListener {
    private final EditText mField;
    private boolean mTextVisible = false;

    public PasswordToggler(EditText field) {
        mField = field;
        mField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    @Override
    public void onClick(View view) {
        ImageView imageView = (ImageView) view;
        mTextVisible = !mTextVisible;
        if (mTextVisible) {
            imageView.setImageResource(R.drawable.ic_visibility_off_black_24dp);
            mField.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            imageView.setImageResource(R.drawable.ic_visibility_black_24dp);
            mField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }
}
