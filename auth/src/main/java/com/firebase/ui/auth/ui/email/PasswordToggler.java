package com.firebase.ui.auth.ui.email;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.firebase.ui.auth.R;

public class PasswordToggler implements ImageView.OnClickListener{
    private EditText mField;
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
            imageView.setImageResource(R.drawable.ic_visibility_black_24dp);
            mField.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            imageView.setImageResource(R.drawable.ic_visibility_off_black_24dp);
            mField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }
}
