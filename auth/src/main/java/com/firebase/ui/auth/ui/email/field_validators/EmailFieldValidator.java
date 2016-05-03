package com.firebase.ui.auth.ui.email.field_validators;

import android.support.design.widget.TextInputLayout;
import android.util.Patterns;

import com.firebase.ui.auth.R;

public class EmailFieldValidator extends BaseValidator {

    public EmailFieldValidator(TextInputLayout errorContainer) {
        super(errorContainer);
        mErrorMessage = mErrorContainer.getContext().getResources().getString(R.string
                .invalid_email_address);
    }

    @Override
    protected boolean isValid(CharSequence charSequence) {
        return Patterns.EMAIL_ADDRESS.matcher(charSequence).matches();
    }

}
