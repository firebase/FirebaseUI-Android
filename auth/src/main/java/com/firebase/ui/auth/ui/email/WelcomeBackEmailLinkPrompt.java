package com.firebase.ui.auth.ui.email;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.TextHelper;
import com.firebase.ui.auth.viewmodel.RequestCodes;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WelcomeBackEmailLinkPrompt extends AppCompatBase implements View.OnClickListener {


    private IdpResponse mIdpResponseForLinking;
    private Button mSignInButton;
    private ProgressBar mProgressBar;


    public static Intent createIntent(
            Context context, FlowParameters flowParams, IdpResponse response) {
        return createBaseIntent(context, WelcomeBackEmailLinkPrompt.class, flowParams)
                .putExtra(ExtraConstants.IDP_RESPONSE, response);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_welcome_back_email_link_prompt_layout);
        mIdpResponseForLinking = IdpResponse.fromResultIntent(getIntent());
        initializeViewObjects();
        setBodyText();
        setOnClickListeners();
        setPrivacyFooter();
    }

    private void startEmailLinkFlow() {
        Intent intent = EmailActivity.createIntentForLinking(this, getFlowParams(),
                mIdpResponseForLinking);
        startActivityForResult(intent, RequestCodes.WELCOME_BACK_EMAIL_LINK_FLOW);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish(resultCode, data);
    }

    private void initializeViewObjects() {
        mSignInButton = findViewById(R.id.button_sign_in);
        mProgressBar = findViewById(R.id.top_progress_bar);
    }

    @SuppressWarnings("WrongConstant")
    private void setBodyText() {
        TextView body = findViewById(R.id.welcome_back_email_link_body);
        String bodyText = getString(R.string.fui_welcome_back_email_link_prompt_body,
                mIdpResponseForLinking.getEmail(),
                mIdpResponseForLinking
                        .getProviderType());

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        // bold email & provider text
        TextHelper.boldAllOccurencesOfText(spannableStringBuilder, bodyText,
                mIdpResponseForLinking.getEmail());
        TextHelper.boldAllOccurencesOfText(spannableStringBuilder, bodyText,
                mIdpResponseForLinking.getProviderType());

        body.setText(spannableStringBuilder);
        // Justifies the text
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            body.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
    }

    private void setOnClickListeners() {
        mSignInButton.setOnClickListener(this);
    }

    private void setPrivacyFooter() {
        TextView footerText = findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(this, getFlowParams(), footerText);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.button_sign_in) {
            startEmailLinkFlow();
        }
    }

    @Override
    public void showProgress(int message) {
        mSignInButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mProgressBar.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
