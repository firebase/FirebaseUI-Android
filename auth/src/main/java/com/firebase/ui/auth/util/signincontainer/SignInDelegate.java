package com.firebase.ui.auth.util.signincontainer;

import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.FragmentHelper;
import com.firebase.ui.auth.ui.User;
import com.firebase.ui.auth.ui.email.RegisterEmailActivity;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.firebase.auth.EmailAuthProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attempts to acquire a credential from Smart Lock for Passwords to sign in
 * an existing account. If this succeeds, an attempt is made to sign the user in
 * with this credential. If it does not, the
 * {@link AuthMethodPickerActivity authentication method picker activity}
 * is started, unless only email is supported, in which case the
 * {@link RegisterEmailActivity} is started.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SignInDelegate extends SignInDelegateBase {
    private static final String TAG = "SignInDelegate";

    public static SignInDelegate getInstance(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment instanceof SignInDelegate) {
            return (SignInDelegate) fragment;
        } else {
            return null;
        }
    }

    public static void delegate(FragmentActivity activity, FlowParameters params) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (!(fragment instanceof SignInDelegate)) {
            SignInDelegate result = new SignInDelegate();
            result.setArguments(FragmentHelper.getFlowParamsBundle(params));
            fm.beginTransaction().add(result, TAG).disallowAddToBackStack().commit();
        }
    }


    @Override
    protected void startAuthMethodChoice() {
        FlowParameters flowParams = mHelper.getFlowParams();
        List<AuthUI.IdpConfig> idpConfigs = flowParams.providerInfo;
        Map<String, AuthUI.IdpConfig> providerIdToConfig = new HashMap<>();
        for (AuthUI.IdpConfig providerConfig : idpConfigs) {
            providerIdToConfig.put(providerConfig.getProviderId(), providerConfig);
        }

        List<AuthUI.IdpConfig> visibleProviders = new ArrayList<>();
        if (flowParams.isReauth) {
            // For reauth flow we only want to show the IDPs which the user has associated with
            // their account.
            List<String> providerIds = mHelper.getCurrentUser().getProviders();
            if (providerIds.size() == 0) {
                // zero providers indicates that it is an email account
                visibleProviders.add(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());
            } else {
                for (String providerId : providerIds) {
                    AuthUI.IdpConfig idpConfig = providerIdToConfig.get(providerId);
                    if (idpConfig == null) {
                        Log.e(TAG, "User has provider " + providerId + " associated with their "
                                + "account, but only the following IDPs have been configured: "
                                + TextUtils.join(", ", providerIdToConfig.keySet()));
                    } else {
                        visibleProviders.add(idpConfig);
                    }
                }
            }
        } else {
            visibleProviders = idpConfigs;
        }
        // If the only provider is Email, immediately launch the email flow. Otherwise, launch
        // the auth method picker screen.
        if (visibleProviders.size() == 1) {
            if (visibleProviders.get(0).getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                startActivityForResult(
                        RegisterEmailActivity.createIntent(getContext(), flowParams),
                        RC_EMAIL_FLOW);
            } else {
                String email = flowParams.isReauth ? mHelper.getCurrentUser().getEmail() : null;
                redirectToIdpSignIn(email,
                                    providerIdToAccountType(visibleProviders.get(0)
                                                                    .getProviderId()));
            }
        } else {
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            flowParams),
                    RC_AUTH_METHOD_PICKER);
        }
        mHelper.dismissDialog();
    }

    @Override
    protected void redirectToIdpSignIn(String email, String accountType) {
        if (TextUtils.isEmpty(accountType)) {
            startActivityForResult(
                    RegisterEmailActivity.createIntent(
                            getContext(),
                            mHelper.getFlowParams(),
                            email),
                    RC_EMAIL_FLOW);
            return;
        }

        if (accountType.equals(IdentityProviders.GOOGLE)
                || accountType.equals(IdentityProviders.FACEBOOK)
                || accountType.equals(IdentityProviders.TWITTER)) {
            IdpSignInContainer.signIn(
                    getActivity(),
                    mHelper.getFlowParams(),
                    new User.Builder(email)
                            .setProvider(accountTypeToProviderId(accountType))
                            .build());
        } else {
            Log.w(TAG, "Unknown provider: " + accountType);
            startActivityForResult(
                    AuthMethodPickerActivity.createIntent(
                            getContext(),
                            mHelper.getFlowParams()),
                    RC_IDP_SIGNIN);
            mHelper.dismissDialog();
        }
    }
}
