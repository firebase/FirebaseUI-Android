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

package com.firebase.ui.auth.util.ui.fieldvalidators;

import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaseValidator {
    protected TextInputLayout mErrorContainer;
    protected String mErrorMessage = "";
    protected String mEmptyMessage;

    public BaseValidator(TextInputLayout errorContainer) {
        mErrorContainer = errorContainer;
    }

    protected boolean isValid(CharSequence charSequence) {
        return true;
    }

    public boolean validate(CharSequence charSequence) {
        if (mEmptyMessage != null && (charSequence == null || charSequence.length() == 0)) {
            mErrorContainer.setError(mEmptyMessage);
            return false;
        } else if (isValid(charSequence)) {
            mErrorContainer.setError("");
            return true;
        } else {
            mErrorContainer.setError(mErrorMessage);
            return false;
        }
    }
}
