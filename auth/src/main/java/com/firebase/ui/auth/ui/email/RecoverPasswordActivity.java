package com.firebase.ui.auth.ui.email;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;

public class RecoverPasswordActivity extends EmailFlowBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.forgot_password);
        setContentView(R.layout.forget_password_layout);
        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);

        mEmailEditText = (EditText) findViewById(R.id.email);
        Button nextButton = (Button) findViewById(R.id.button_done);

        if (email != null) {
            mEmailEditText.setText(email);
        }
        nextButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        if(super.isPendingFinishing.get()) {
            return;
        }
        if (view.getId() == R.id.button_done) {
            if(mEmailEditText.getText().toString().equalsIgnoreCase("") ) {
                Toast.makeText(this, R.string.require_email_text, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent data = new Intent();
            data.putExtra(ControllerConstants.EXTRA_EMAIL, mEmailEditText.getText().toString());
            finish(RESULT_OK, data);
        }
    }

    public static Intent createIntent(Context context, String appName, String email) {
        return new Intent().setClass(context, RecoverPasswordActivity.class)
                .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
                .putExtra(ControllerConstants.EXTRA_EMAIL, email);
    }
}
