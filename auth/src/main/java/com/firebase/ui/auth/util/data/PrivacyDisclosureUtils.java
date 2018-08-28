package com.firebase.ui.auth.util.data;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.util.ui.PreambleHandler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PrivacyDisclosureUtils {

    private static final int NO_TOS_OR_PP = -1;

    public static void setupTermsOfServiceAndPrivacyPolicyText(Context context,
                                                               FlowParameters flowParameters,
                                                               TextView termsText) {
        PreambleHandler.setup(context,
                flowParameters,
                getGlobalTermsStringResource(flowParameters),
                termsText);
    }

    public static void setupTermsOfServiceFooter(Context context,
                                                 FlowParameters flowParameters,
                                                 TextView footerText) {
        PreambleHandler.setup(context,
                flowParameters,
                getGlobalTermsFooterStringResource(flowParameters),
                footerText);
    }

    public static void setupTermsOfServiceAndPrivacyPolicySmsText(Context context,
                                                                  FlowParameters flowParameters,
                                                                  TextView termsText) {
        PreambleHandler.setup(context,
                flowParameters,
                R.string.fui_verify_phone_number,
                getTermsSmsStringResource(flowParameters),
                termsText);
    }

    @StringRes
    private static int getGlobalTermsStringResource(FlowParameters flowParameters) {
        boolean termsOfServiceUrlProvided = flowParameters.isTermsOfServiceUrlProvided();
        boolean privacyPolicyUrlProvided = flowParameters.isPrivacyPolicyUrlProvided();

        if (termsOfServiceUrlProvided && privacyPolicyUrlProvided) {
            return R.string.fui_tos_and_pp;
        }

        return NO_TOS_OR_PP;
    }

    @StringRes
    private static int getGlobalTermsFooterStringResource(FlowParameters flowParameters) {
        boolean termsOfServiceUrlProvided = flowParameters.isTermsOfServiceUrlProvided();
        boolean privacyPolicyUrlProvided = flowParameters.isPrivacyPolicyUrlProvided();

        if (termsOfServiceUrlProvided && privacyPolicyUrlProvided) {
            return R.string.fui_tos_and_pp_footer;
        }

        return NO_TOS_OR_PP;
    }

    @StringRes
    private static int getTermsSmsStringResource(FlowParameters flowParameters) {
        boolean termsOfServiceUrlProvided = flowParameters.isTermsOfServiceUrlProvided();
        boolean privacyPolicyUrlProvided = flowParameters.isPrivacyPolicyUrlProvided();

        if (termsOfServiceUrlProvided && privacyPolicyUrlProvided) {
            return R.string.fui_sms_terms_of_service_and_privacy_policy_extended;
        }

        return NO_TOS_OR_PP;
    }
}
