package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;

/**
 * TODO javadoc
 */
public class EditPasswordActivity extends SaveFieldActivity {
    public static Intent createIntent(Context context, FlowParameters flowParameters) {
        return BaseHelper.createBaseIntent(context, EditPasswordActivity.class, flowParameters);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_edit_password);
    }

    @Override
    protected void onSaveMenuItem() {

    }
}
