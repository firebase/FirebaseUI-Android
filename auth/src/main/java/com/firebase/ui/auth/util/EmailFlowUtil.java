package com.firebase.ui.auth.util;

import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.firebase.ui.auth.ui.email.SignInNoPasswordActivity;
import com.firebase.ui.auth.util.smartlock.SignInDelegate;

/**
 * Helper class to kick off the Email/Password sign-in flow.
 */
public class EmailFlowUtil {
    /**
     * Return an intent for either {@link EmailHintContainer} or
     * {@link SignInNoPasswordActivity} depending on if SmartLock is enabled.
     */
    public static EmailHintContainer startEmailFlow(FragmentActivity activity,
                                      @Nullable SignInDelegate signInDelegate,
                                      BaseHelper helper,
                                      int requestCode) {
        if (helper.getFlowParams().smartLockEnabled) {
            if (signInDelegate != null) {
                return new EmailHintContainer(helper).trySignInWithEmailAndPassword(signInDelegate);
            } else {
                return new EmailHintContainer(helper).trySignInWithEmailAndPassword(activity);
            }
        } else {
            if (signInDelegate != null) {
                signInDelegate.startActivityForResult(
                        SignInNoPasswordActivity.createIntent(signInDelegate.getContext(), helper.getFlowParams(), null),
                        requestCode);
            } else {
                activity.startActivityForResult(
                        SignInNoPasswordActivity.createIntent(activity, helper.getFlowParams(), null),
                        requestCode);
            }
        }
        return null;
    }
}
