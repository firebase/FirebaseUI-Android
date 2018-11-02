package com.firebase.ui.auth.ui.email;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.AppCompatBase;

/** Handles the recovery flow for finishing the cross-device email link sign in flow. We either
 * need the user to input their email, or we need them to determine if they want to continue
 * the linking flow. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailLinkErrorRecoveryActivity extends AppCompatBase
        implements EmailLinkEmailPromptFragment.EmailLinkSignInListener {

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return createBaseIntent(context, EmailLinkErrorRecoveryActivity.class, flowParams);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_activity_register_email);

        switchFragment(new EmailLinkEmailPromptFragment(), EmailLinkEmailPromptFragment.TAG);
    }

    private void switchFragment(Fragment fragment, String tag, boolean withTransition) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (withTransition) {
            ft.setCustomAnimations(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
        ft.replace(R.id.fragment_register_email, fragment, tag).disallowAddToBackStack().commit();
    }

    private void switchFragment(Fragment fragment, String tag) {
        switchFragment(fragment, tag, false);
    }

    @Override
    public void onSuccess(IdpResponse response) {
        finish(RESULT_OK, response.toIntent());
    }

    @Override
    public void showProgress(@StringRes int message) {
        throw new UnsupportedOperationException("Email fragments must handle progress updates.");
    }

    @Override
    public void hideProgress() {
        throw new UnsupportedOperationException("Email fragments must handle progress updates.");
    }
}
