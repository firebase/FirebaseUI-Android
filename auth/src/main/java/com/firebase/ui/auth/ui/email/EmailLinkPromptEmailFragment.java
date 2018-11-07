package com.firebase.ui.auth.ui.email;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.email.EmailLinkSignInHandler;

/** Prompts the user to enter their email to finish the cross-device email link sign in flow. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkPromptEmailFragment extends FragmentBase implements
        View.OnClickListener {

    public static final String TAG = "EmailLinkPromptEmailFragment";

    private Button mNextButton;
    private ProgressBar mProgressBar;

    private EditText mEmailEditText;
    private TextInputLayout mEmailLayout;
    private EmailFieldValidator mEmailFieldValidator;

    private EmailLinkSignInHandler mHandler;
    private EmailLinkPromptEmailListener mListener;

    public static EmailLinkPromptEmailFragment newInstance() {
        EmailLinkPromptEmailFragment fragment = new EmailLinkPromptEmailFragment();
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_check_email_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mNextButton = view.findViewById(R.id.button_next);
        mProgressBar = view.findViewById(R.id.top_progress_bar);

        mNextButton.setOnClickListener(this);

        // Email field and validator
        mEmailLayout = view.findViewById(R.id.email_layout);
        mEmailEditText = view.findViewById(R.id.email);
        mEmailFieldValidator = new EmailFieldValidator(mEmailLayout);
        mEmailLayout.setOnClickListener(this);
        mEmailEditText.setOnClickListener(this);

        // Set body text
        LinearLayout topLayout = view.findViewById(R.id.email_top_layout);
        TextView textView = new TextView(getActivity());
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(R.style.FirebaseUI_Text_Heading);
        }
        textView.setText(R.string.fui_email_link_confirm_email_message);
        topLayout.addView(textView, 0);

        // Set activity title
        getActivity().setTitle(R.string.fui_email_link_confirm_email_header);

        // Set Tos/Pp footer
        TextView footerText = view.findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(), getFlowParams(),
                footerText);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (!(activity instanceof EmailLinkPromptEmailListener)) {
            throw new IllegalStateException("Activity must implement EmailLinkPromptEmailListener");
        }
        mListener = (EmailLinkPromptEmailListener) activity;

        initHandler();
    }

    private void initHandler() {
        mHandler = ViewModelProviders.of(this).get(EmailLinkSignInHandler.class);
        mHandler.init(getFlowParams());
        mHandler.getOperation().observe(this, new ResourceObserver<IdpResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdpResponse response) {
                mListener.onEmailPromptSuccess(response);
            }

            @Override
            protected void onFailure(@NonNull final Exception e) {
                // We've checked the oob code before starting this flow via #checkActionCode.
                // I don't see this failing in a non-recoverable way.
                mEmailLayout.setError(e.getMessage());
            }
        });
    }

    private void validateEmailAndFinishSignIn() {
        String email = mEmailEditText.getText().toString();
        if (mEmailFieldValidator.validate(email)) {
            mHandler.finishSignIn(email);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_next) {
            validateEmailAndFinishSignIn();
        } else if (id == R.id.email_layout || id == R.id.email) {
            mEmailLayout.setError(null);
        }
    }

    @Override
    public void showProgress(int message) {
        mNextButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mNextButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
    interface EmailLinkPromptEmailListener {
        /* Pass on the success to the hosting Activity so we can complete the sign in */
        void onEmailPromptSuccess(IdpResponse response);
    }
}
