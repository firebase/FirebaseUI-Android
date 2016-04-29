package com.firebase.ui.auth.ui.email;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;

public class ConfirmRecoverPasswordActivity extends EmailFlowBaseActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_recovery_layout);
        setTitle(R.string.check_your_email);
        boolean isSuccess = getIntent().getBooleanExtra(ControllerConstants.EXTRA_SUCCESS, true);

        if (!isSuccess) {
            ((TextView) findViewById(R.id.title)).setText(R.string.recovery_fail_title);
        }
        findViewById(R.id.button_done).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(super.isPendingFinishing.get()) {
            return;
        }
        if (view.getId() == R.id.button_done) {
            finish(RESULT_OK, new Intent());
        }
    }
}
