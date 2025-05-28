/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.util.data;

import android.content.Context;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.util.ui.PreambleHandler;

import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;

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
