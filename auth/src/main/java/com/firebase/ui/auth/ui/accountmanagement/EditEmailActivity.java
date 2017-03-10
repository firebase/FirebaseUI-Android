package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;

import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.fieldvalidators.EmailFieldValidator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * TODO javadoc
 */
public class EditEmailActivity extends SaveFieldActivity {
    private static final String TAG = "EditEmailAct";
    private EmailFieldValidator mValidator;
    private EditText mEmailField;
    private TextInputLayout mEmailInputLayout;

    public static Intent createIntent(Context context, FlowParameters flowParameters) {
        return BaseHelper.createBaseIntent(context, EditEmailActivity.class, flowParameters);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_edit_email);
        mEmailField = (EditText) findViewById(R.id.email);
        mEmailField.setText(mActivityHelper.getCurrentUser().getEmail());
        mEmailInputLayout = (TextInputLayout) findViewById(R.id.email_layout);
        mValidator = new EmailFieldValidator(mEmailInputLayout);

    }

    @Override
    protected void onSaveMenuItem() {
        if (mValidator.validate(mEmailField.getText())) {
            mActivityHelper.getCurrentUser().updateEmail(mEmailField.getText().toString())
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "failure updating email"))
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mEmailInputLayout.setError(e.getLocalizedMessage());
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            finish();
                        }
                    });
        }
    }
}
