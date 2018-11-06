package com.firebase.ui.auth.ui.email;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;

/**
 * Handles the recovery flow for finishing the cross-device email link sign in flow. We either
 * need the user to input their email, or we need them to determine if they want to continue
 * the linking flow.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkErrorRecoveryActivity extends AppCompatBase
        implements EmailLinkEmailPromptFragment.EmailLinkPromptEmailListener,
        EmailLinkCrossDeviceLinkingFragment.FinishEmailLinkSignInListener {

    private static final String RECOVERY_TYPE_KEY = "com.firebase.ui.auth.ui.email.recoveryTypeKey";

    public static Intent createIntent(Context context, FlowParameters flowParams, int flow) {
        return createBaseIntent(context, EmailLinkErrorRecoveryActivity.class, flowParams)
                .putExtra(RECOVERY_TYPE_KEY, flow);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_register_email);

        setTitle(R.string.fui_sign_in_default);

        // Needed so that a rotation doesn't cause us to land on the wrong fragment
        if (savedInstanceState != null) {
            if (!getSupportFragmentManager().getFragments().isEmpty()) {
                if (getSupportFragmentManager().getFragments().get(0)
                        instanceof EmailLinkEmailPromptFragment) {
                    setTitle(R.string.fui_email_link_confirm_email_header);
                }
            }
            return;
        }

        boolean linkingFlow = getIntent().getIntExtra(RECOVERY_TYPE_KEY, -1)
                == RequestCodes.EMAIL_LINK_CROSS_DEVICE_LINKING_FLOW;

        Fragment fragment;
        if (linkingFlow) {
            fragment = EmailLinkCrossDeviceLinkingFragment.newInstance();
        } else {
            setTitle(R.string.fui_email_link_confirm_email_header);
            fragment = EmailLinkEmailPromptFragment.newInstance();
        }
        switchFragment(fragment, R.id.fragment_register_email, EmailLinkEmailPromptFragment.TAG);
    }

    @Override
    public void onSuccess(IdpResponse response) {
        finish(RESULT_OK, response.toIntent());
    }

    @Override
    public void completeCrossDeviceEmailLinkFlow() {
        setTitle(R.string.fui_email_link_confirm_email_header);
        EmailLinkEmailPromptFragment fragment
                = EmailLinkEmailPromptFragment.newInstance();
        switchFragment(fragment, R.id.fragment_register_email,
                EmailLinkCrossDeviceLinkingFragment.TAG, /*withTransition=*/true,
                /*addToBackStack=*/true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setTitle(R.string.fui_sign_in_default);
    }

    @Override
    public void showProgress(@StringRes int message) {
        throw new UnsupportedOperationException("Fragments must handle progress updates.");
    }

    @Override
    public void hideProgress() {
        throw new UnsupportedOperationException("Fragments must handle progress updates.");
    }
}
