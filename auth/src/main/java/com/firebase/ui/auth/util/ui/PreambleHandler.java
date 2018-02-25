package com.firebase.ui.auth.util.ui;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PreambleHandler {
    private static final String BTN_TARGET = "%BTN%";
    private static final String TOS_TARGET = "%TOS%";
    private static final String PP_TARGET = "%PP%";

    private final Context mContext;
    private final FlowParameters mFlowParameters;
    private final int mButtonText;
    private final ForegroundColorSpan mLinkSpan;

    private SpannableStringBuilder mBuilder;

    private PreambleHandler(Context context, FlowParameters parameters, @StringRes int buttonText) {
        mContext = context;
        mFlowParameters = parameters;
        mButtonText = buttonText;
        mLinkSpan = new ForegroundColorSpan(ContextCompat.getColor(mContext,
                R.color.fui_linkColor));
    }

    public static void setup(Context context,
                             FlowParameters parameters,
                             @StringRes int buttonText,
                             TextView textView) {
        PreambleHandler handler = new PreambleHandler(context, parameters, buttonText);
        handler.setupCreateAccountPreamble();
        handler.setPreamble(textView);
    }

    private void setPreamble(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(mBuilder);
    }

    private void setupCreateAccountPreamble() {
        String withTargets = getPreambleStringWithTargets();
        if (withTargets == null) {
            return;
        }

        mBuilder = new SpannableStringBuilder(withTargets);

        replaceTarget(BTN_TARGET, mButtonText);
        replaceUrlTarget(
                TOS_TARGET,
                R.string.fui_terms_of_service,
                mFlowParameters.termsOfServiceUrl);
        replaceUrlTarget(PP_TARGET, R.string.fui_privacy_policy, mFlowParameters.privacyPolicyUrl);
    }

    private void replaceTarget(String target, @StringRes int replacementRes) {
        int targetIndex = mBuilder.toString().indexOf(target);
        if (targetIndex != -1) {
            String replacement = mContext.getString(replacementRes);
            mBuilder.replace(targetIndex, targetIndex + target.length(), replacement);
        }
    }

    private void replaceUrlTarget(String target, @StringRes int replacementRes, String url) {
        int targetIndex = mBuilder.toString().indexOf(target);
        if (targetIndex != -1) {
            String replacement = mContext.getString(replacementRes);
            mBuilder.replace(targetIndex, targetIndex + target.length(), replacement);

            int end = targetIndex + replacement.length();
            mBuilder.setSpan(mLinkSpan, targetIndex, end, 0);
            mBuilder.setSpan(new CustomTabsSpan(url), targetIndex, end, 0);
        }
    }

    @Nullable
    private String getPreambleStringWithTargets() {
        boolean hasTos = !TextUtils.isEmpty(mFlowParameters.termsOfServiceUrl);
        boolean hasPp = !TextUtils.isEmpty(mFlowParameters.privacyPolicyUrl);

        if (hasTos && hasPp) {
            return mContext.getString(R.string.fui_create_account_preamble_tos_and_pp,
                                      BTN_TARGET, TOS_TARGET, PP_TARGET);
        } else if (hasTos) {
            return mContext.getString(R.string.fui_create_account_preamble_tos_only,
                                      BTN_TARGET, TOS_TARGET);
        } else if (hasPp) {
            return mContext.getString(R.string.fui_create_account_preamble_pp_only,
                                      BTN_TARGET, PP_TARGET);
        } else {
            return null;
        }
    }

    private class CustomTabsSpan extends ClickableSpan {
        private final String mUrl;
        private final CustomTabsIntent mCustomTabsIntent;

        public CustomTabsSpan(String url) {
            mUrl = url;

            // Getting default color
            TypedValue typedValue = new TypedValue();
            mContext.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            @ColorInt int color = typedValue.data;

            mCustomTabsIntent = new CustomTabsIntent.Builder()
                    .setToolbarColor(color)
                    .setShowTitle(true)
                    .build();
        }

        @Override
        public void onClick(View widget) {
            mCustomTabsIntent.launchUrl(mContext, Uri.parse(mUrl));
        }
    }
}
