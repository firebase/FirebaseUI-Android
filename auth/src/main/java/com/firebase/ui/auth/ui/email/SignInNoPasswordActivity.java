package com.firebase.ui.auth.ui.email;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.ui.BaseActivity;

public class SignInNoPasswordActivity extends EmailFlowBaseActivity implements View.OnClickListener {
    private EditText mEmailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.enter_your_email);
        setContentView(R.layout.signin_no_password_layout);

        String email = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);

        mEmailEditText = (EditText) findViewById(R.id.email);

        if(email != null) {
            mEmailEditText.setText(email);
        }

        Button button = (Button) findViewById(R.id.button_ok);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (super.isPendingFinishing.get()) {
            return;
        }
        String email = mEmailEditText.getText().toString();
        Intent dataExtra = new Intent();
        dataExtra.putExtra(ControllerConstants.EXTRA_EMAIL, email);
        finish(BaseActivity.RESULT_OK, dataExtra);
    }
}
