package com.firebase.ui.auth.util.signincontainer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentHelper;
import com.firebase.ui.auth.util.signincontainer.SignInDelegateBase;

/**
 * TODO javadoc
 */
public class ReauthDelegate extends SignInDelegateBase {
    private static final String TAG = "ReauthDelegate";


    public static ReauthDelegate getInstance(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment instanceof ReauthDelegate) {
            return (ReauthDelegate) fragment;
        } else {
            return null;
        }
    }

    @Override
    protected void startAuthMethodChoice() {
        mHelper.dismissDialog();
    }

    public static void delegate(FragmentActivity activity, FlowParameters params) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (!(fragment instanceof ReauthDelegate)) {
            ReauthDelegate result = new ReauthDelegate();
            result.setArguments(FragmentHelper.getFlowParamsBundle(params));
            fm.beginTransaction().add(result, TAG).disallowAddToBackStack().commit();
        }
    }

    @Override
    protected void redirectToIdpSignIn(String email, String accountType) {

    }
}
