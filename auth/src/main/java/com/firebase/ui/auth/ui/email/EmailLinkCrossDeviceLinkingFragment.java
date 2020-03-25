package com.firebase.ui.auth.ui.email;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.util.data.EmailLinkParser;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.TextHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.FragmentActivity;

/**
 * Fragment that tells the user that a linking flow cannot be completed as they have opened the
 * email link on a different device.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkCrossDeviceLinkingFragment extends FragmentBase
        implements View.OnClickListener {

    public static final String TAG = "CrossDeviceFragment";

    private FinishEmailLinkSignInListener mListener;
    private ProgressBar mProgressBar;
    private Button mContinueButton;

    public static EmailLinkCrossDeviceLinkingFragment newInstance() {
        EmailLinkCrossDeviceLinkingFragment fragment = new EmailLinkCrossDeviceLinkingFragment();
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fui_email_link_cross_device_linking, container, false);
    }

    @SuppressWarnings("WrongConstant")
    @Override
    @SuppressLint("WrongConstant")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mProgressBar = view.findViewById(R.id.top_progress_bar);
        mContinueButton = view.findViewById(R.id.button_continue);
        mContinueButton.setOnClickListener(this);

        String link = getFlowParams().emailLink;

        EmailLinkParser parser = new EmailLinkParser(link);

        String providerId = parser.getProviderId();
        String providerName = ProviderUtils.providerIdToProviderName(providerId);

        TextView body = view.findViewById(R.id.cross_device_linking_body);
        String bodyText = getString(R.string.fui_email_link_cross_device_linking_text,
                providerName);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodyText);
        TextHelper.boldAllOccurencesOfText(spannableStringBuilder, bodyText, providerName);
        body.setText(spannableStringBuilder);

        // Justifies the text
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            body.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
        }

        TextView footerText = view.findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(), getFlowParams(),
                footerText);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (!(activity instanceof FinishEmailLinkSignInListener)) {
            throw new IllegalStateException("Activity must implement EmailLinkPromptEmailListener");
        }
        mListener = (FinishEmailLinkSignInListener) activity;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_continue) {
            mListener.completeCrossDeviceEmailLinkFlow();
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


    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
    interface FinishEmailLinkSignInListener {
        /**
         * Used to let the hosting activity know that we can finish the email link sign in flow
         */
        void completeCrossDeviceEmailLinkFlow();
    }
}
