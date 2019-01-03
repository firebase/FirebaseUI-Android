package com.firebase.ui.auth.util.ui;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;
import android.widget.EditText;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaselineTextInputLayout extends TextInputLayout {
    public BaselineTextInputLayout(Context context) {
        super(context);
    }

    public BaselineTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaselineTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getBaseline() {
        EditText text = getEditText();
        return text == null ? super.getBaseline() : text.getPaddingTop() + text.getBaseline();
    }
}
