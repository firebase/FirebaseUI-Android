package com.firebase.ui.auth.ui.email.field_validators;

import android.support.design.widget.TextInputLayout;

import com.firebase.ui.auth.R;

public class RequiredFieldValidator extends BaseValidator {
    public RequiredFieldValidator(TextInputLayout errorContainer) {
        super(errorContainer);
        mErrorMessage = mErrorContainer.getContext().getResources().getString(R.string
                .required_field);
    }

    @Override
    protected boolean isValid(CharSequence charSequence) {
        return charSequence != null && charSequence.length() > 0;
    }
}
