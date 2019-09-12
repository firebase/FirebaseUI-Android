package com.firebase.ui.auth.ui.email;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.FragmentActivity;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TroubleSigningInFragment extends FragmentBase implements View.OnClickListener {

    public static final String TAG = "TroubleSigningInFragment";

    private ResendEmailListener mListener;
    private ProgressBar mProgressBar;

    private String mEmail;

    public static TroubleSigningInFragment newInstance(@NonNull final String email) {
        TroubleSigningInFragment fragment = new TroubleSigningInFragment();
        Bundle args = new Bundle();
        args.putString(ExtraConstants.EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_email_link_trouble_signing_in_layout, container,
                false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (!(activity instanceof ResendEmailListener)) {
            throw new IllegalStateException("Activity must implement ResendEmailListener");
        }
        mListener = (ResendEmailListener) activity;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mProgressBar = view.findViewById(R.id.top_progress_bar);
        mEmail = getArguments().getString(ExtraConstants.EMAIL);

        setOnClickListeners(view);
        setPrivacyFooter(view);
    }

    private void setOnClickListeners(View view) {
        view.findViewById(R.id.button_resend_email).setOnClickListener(this);
    }

    private void setPrivacyFooter(View view) {
        TextView footerText = view.findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(), getFlowParams(),
                footerText);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_resend_email) {
            mListener.onClickResendEmail(mEmail);
        }
    }

    @Override
    public void showProgress(int message) {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    interface ResendEmailListener {
        /**
         * User clicks on the resend email button.
         */
        void onClickResendEmail(String email);
    }
}
