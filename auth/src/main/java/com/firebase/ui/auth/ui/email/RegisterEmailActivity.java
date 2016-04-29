package com.firebase.ui.auth.ui.email;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;

public class RegisterEmailActivity extends EmailFlowBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mNameEditText;

    @Override
    public void onBackPressed() {
        String email = mEmailEditText.getText().toString();
        Intent data = new Intent();
        data.putExtra(ControllerConstants.EXTRA_EMAIL, email);
        finish(BACK_IN_FLOW, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_email_layout);

        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
        mEmailEditText = (EditText) findViewById(R.id.email);
        mPasswordEditText = (EditText) findViewById(R.id.password);
        mNameEditText = (EditText) findViewById(R.id.name);

        if (email != null) {
            mEmailEditText.setText(email);
        }

        Button createButton = (Button) findViewById(R.id.button_create);
        createButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(super.isPendingFinishing.get()) {
            return;
        }
       if (view.getId() == R.id.button_create) {
           Intent data = new Intent();
           data.putExtra(ControllerConstants.EXTRA_EMAIL, mEmailEditText.getText().toString());
           data.putExtra(ControllerConstants.EXTRA_NAME, mNameEditText.getText().toString());
           data.putExtra(ControllerConstants.EXTRA_PASSWORD, mPasswordEditText.getText().toString());
           finish(RESULT_OK, data);
       }
    }
}
