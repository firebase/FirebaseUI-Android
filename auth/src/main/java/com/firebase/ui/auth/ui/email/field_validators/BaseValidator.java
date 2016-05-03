package com.firebase.ui.auth.ui.email.field_validators;

import android.support.design.widget.TextInputLayout;

public class BaseValidator {
    protected TextInputLayout mErrorContainer;
    protected String mErrorMessage = "";

    public BaseValidator(TextInputLayout errorContainer) {
        mErrorContainer = errorContainer;
    }

    protected boolean isValid(CharSequence charSequence) {
        return true;
    }

    public boolean validate(CharSequence charSequence) {
        if (isValid(charSequence)) {
            mErrorContainer.setError("");
            return true;
        } else {
            mErrorContainer.setError(mErrorMessage);
            return false;
        }
    }
}
