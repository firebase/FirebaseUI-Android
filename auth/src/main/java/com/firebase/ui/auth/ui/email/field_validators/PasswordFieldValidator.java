package com.firebase.ui.auth.ui.email.field_validators;

import android.support.design.widget.TextInputLayout;

import com.firebase.ui.auth.R;

public class PasswordFieldValidator extends BaseValidator {
    private int mMinLength;

    public PasswordFieldValidator(TextInputLayout errorContainer, int minLength) {
        super(errorContainer);
        mMinLength = minLength;
        String template = mErrorContainer.getResources().getString(R.string.password_length);
        mErrorMessage = String.format(template, mMinLength);
    }

    @Override
    protected boolean isValid(CharSequence charSequence) {
        return charSequence.length() >= mMinLength;
    }
}
