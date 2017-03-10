package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.UserProfileChangeRequest;

public class EditDisplayNameActivity extends SaveFieldActivity {
    private static final String TAG = "EditDisplayNameAct";
    private EditText mDisplayName;

    public static Intent createIntent(Context context, FlowParameters flowParameters) {
        return BaseHelper.createBaseIntent(context, EditDisplayNameActivity.class, flowParameters);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_display_name);
        mDisplayName = (EditText) findViewById(R.id.display_name);
        mDisplayName.setText(mActivityHelper.getCurrentUser().getDisplayName());
    }

    @Override
    protected void onSaveMenuItem() {
        mActivityHelper.getCurrentUser().updateProfile(
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(mDisplayName.getText().toString())
                        .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        finish();
                    }
                })
                .addOnFailureListener(new TaskFailureLogger(TAG, "Failed to save display name"));
    }
}
