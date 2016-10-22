package com.firebase.ui.auth.util;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.idp.EmailHintContainer;
import com.firebase.ui.auth.ui.email.SignInNoPasswordActivity;

/**
 * Helper class to kick off the Email/Password sign-in flow.
 */
public class EmailFlowUtil {
    /**
     * Return an intent for either {@link EmailHintContainer} or
     * {@link SignInNoPasswordActivity} depending on if SmartLock is enabled.
     */
    public static void startEmailFlow(FragmentActivity activity,
                                      @Nullable Fragment fragment,
                                      FlowParameters parameters,
                                      String tag,
                                      int requestCode) {
        if (parameters.smartLockEnabled) {
            EmailHintContainer.getInstance(activity, parameters, tag).trySignInWithEmailAndPassord();
        } else {
            if (fragment != null) {
                fragment.startActivityForResult(
                        SignInNoPasswordActivity.createIntent(fragment.getContext(), parameters, null),
                        requestCode);
            } else {
                activity.startActivityForResult(
                        SignInNoPasswordActivity.createIntent(activity, parameters, null),
                        requestCode);
            }
        }
    }
}
