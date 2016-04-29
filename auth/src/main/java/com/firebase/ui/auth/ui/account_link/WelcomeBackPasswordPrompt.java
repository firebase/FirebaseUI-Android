package com.firebase.ui.auth.ui.account_link;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.choreographer.Controller;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.account_link.AccountLinkController;
import com.firebase.ui.auth.ui.BaseActivity;

public class WelcomeBackPasswordPrompt extends BaseActivity implements View.OnClickListener {
    final StyleSpan bold = new StyleSpan(Typeface.BOLD);
    private String mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.sign_in_title);
        setContentView(R.layout.welcome_back_password_prompt_layout);
        mEmail = getIntent().getStringExtra(ControllerConstants.EXTRA_EMAIL);
        String appName = getIntent().getStringExtra(ControllerConstants.EXTRA_APP_NAME);
        TextView bodyTextView = (TextView) findViewById(R.id.welcome_back_password_body);
        String bodyText = getResources().getString(R.string.welcome_back_password_prompt_body);
        bodyText = String.format(bodyText, mEmail, appName);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        int emailStart = bodyText.indexOf(mEmail);
        spannableStringBuilder.setSpan(bold, emailStart, emailStart + mEmail.length(), Spannable
                .SPAN_INCLUSIVE_INCLUSIVE);
        bodyTextView.setText(spannableStringBuilder);
        Button signIn = (Button) findViewById(R.id.button_done);
        signIn.setOnClickListener(this);
        EditText password = (EditText) findViewById(R.id.password);
        TextView troubleSigningIn = (TextView) findViewById(R.id.trouble_signing_in);
        troubleSigningIn.setOnClickListener(this);
        String error = getIntent().getStringExtra(ControllerConstants.EXTRA_ERROR);
        if (error != null) {
            password.setError(error);
        }
    }

    @Override
    protected Controller setUpController() {
        return new AccountLinkController(getApplicationContext());
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_done) {
            EditText password = (EditText) findViewById(R.id.password);
            finish(RESULT_OK, new Intent()
                    .putExtra(ControllerConstants.EXTRA_EMAIL, getIntent()
                            .getStringExtra(ControllerConstants.EXTRA_EMAIL))
                    .putExtra(ControllerConstants.EXTRA_PASSWORD, password.getText().toString())
            );
        } else if (id == R.id.trouble_signing_in) {
            finish(RESULT_FIRST_USER, new Intent().putExtra(
                    ControllerConstants.EXTRA_EMAIL, mEmail));
        }
    }
}
