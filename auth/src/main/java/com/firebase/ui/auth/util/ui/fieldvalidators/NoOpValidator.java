package com.firebase.ui.auth.util.ui.fieldvalidators;

import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;

/**
 * Validator that is always valid.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NoOpValidator extends BaseValidator {

    public NoOpValidator(TextInputLayout errorContainer) {
        super(errorContainer);
    }

    @Override
    protected boolean isValid(CharSequence charSequence) {
        return true;
    }
}
