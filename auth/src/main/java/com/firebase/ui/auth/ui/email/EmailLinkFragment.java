package com.firebase.ui.auth.ui.email;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.ui.TextHelper;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.email.EmailLinkEmailHandler;
import com.google.firebase.auth.ActionCodeSettings;

public class EmailLinkFragment extends FragmentBase {

    public static final String TAG = "EmailLinkFragment";

    private static final String EMAIL_SENT = "emailSent";

    private EmailLinkEmailHandler mHandler;
    private ProgressBar mProgressBar;
    private TroubleSigningInListener mListener;

    public static EmailLinkFragment newInstance(@NonNull final String email, @NonNull final
    ActionCodeSettings actionCodeSettings) {
        EmailLinkFragment fragment = new EmailLinkFragment();
        Bundle args = new Bundle();
        args.putString(ExtraConstants.EMAIL, email);
        args.putParcelable(ExtraConstants.ACTION_CODE_SETTINGS, actionCodeSettings);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (!(activity instanceof TroubleSigningInListener)) {
            throw new IllegalStateException("Activity must implement TroubleSigningInListener");
        }
        mListener = (TroubleSigningInListener) activity;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_email_link_sign_in_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mProgressBar = view.findViewById(R.id.top_progress_bar);
        getView().setVisibility(View.GONE);

        String email = getArguments().getString(ExtraConstants.EMAIL);
        setBodyText(view, email);
        setOnClickListeners(view, email);
        setPrivacyFooter(view);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initHandler();

        String email = getArguments().getString(ExtraConstants.EMAIL);
        ActionCodeSettings actionCodeSettings = getArguments().getParcelable(ExtraConstants
                .ACTION_CODE_SETTINGS);

        if (savedInstanceState == null || !savedInstanceState.getBoolean(EMAIL_SENT)) {
            mHandler.sendSignInLinkToEmail(email, actionCodeSettings);
        }
    }

    private void initHandler() {
        mHandler = ViewModelProviders.of(this).get(EmailLinkEmailHandler.class);
        mHandler.init(getFlowParams());

        mHandler.getOperation().observe(this, new ResourceObserver<String>(this,
                R.string.fui_progress_dialog_sending) {
            @Override
            protected void onSuccess(@NonNull String email) {
                Log.w(TAG, "Email for email link sign in sent successfully.");
                getView().setVisibility(View.VISIBLE);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                // TODO(lsirac): fix
                mListener.onSendEmailFailure();
            }
        });
    }

    private void setBodyText(View view, final String email) {
        TextView body = view.findViewById(R.id.sign_in_email_sent_text);
        String bodyText = getString(R.string.fui_email_link_email_sent, email);

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        TextHelper.boldAllOccurencesOfText(spannableStringBuilder, bodyText, email);
        body.setText(spannableStringBuilder);
    }

    private void setOnClickListeners(View view, final String email) {
        view.findViewById(R.id.trouble_signing_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTroubleSigningIn(email);
            }
        });
    }

    private void setPrivacyFooter(View view) {
        TextView footerText = view.findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(), getFlowParams(),
                footerText);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean(EMAIL_SENT, true);
    }

    @Override
    public void showProgress(int message) {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    interface TroubleSigningInListener {
        /**
         * User clicks on trouble signing in.
         */
        void onTroubleSigningIn(String email);

        /**
         * Failure occurs when trying to send the email.
         */
        void onSendEmailFailure();
    }
}
