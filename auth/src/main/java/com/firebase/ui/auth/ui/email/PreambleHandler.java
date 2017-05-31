package com.firebase.ui.auth.ui.email;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.ColorInt;
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
import com.firebase.ui.auth.ui.FlowParameters;

public class PreambleHandler {
    private static final String BTN_TARGET = "%BTN%";
    private static final String TOS_TARGET = "%TOS%";
    private static final String PP_TARGET = "%PP%";

    private final Context mContext;
    private final FlowParameters mFlowParameters;
    private final int mButtonText;
    private final ForegroundColorSpan mLinkSpan;

    private SpannableStringBuilder mBuilder;

    public PreambleHandler(Context context, FlowParameters parameters, @StringRes int buttonText) {
        mContext = context;
        mFlowParameters = parameters;
        mButtonText = buttonText;
        mLinkSpan = new ForegroundColorSpan(ContextCompat.getColor(mContext, R.color.linkColor));

        setupCreateAccountPreamble();
    }

    public void setPreamble(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(mBuilder);
    }

    private void setupCreateAccountPreamble() {
        int preambleType = getPreambleType();
        if (preambleType == -1) {
            return;
        }

        String[] preambles =
                mContext.getResources().getStringArray(R.array.create_account_preamble);
        mBuilder = new SpannableStringBuilder(preambles[preambleType]);

        replaceTarget(BTN_TARGET, mButtonText);
        replaceUrlTarget(TOS_TARGET, R.string.terms_of_service, mFlowParameters.termsOfServiceUrl);
        replaceUrlTarget(PP_TARGET, R.string.privacy_policy, mFlowParameters.privacyPolicyUrl);
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

    /**
     * 0 means we have both a TOS and a PP
     * <p>1 means we only have a TOS
     * <p>2 means we only have a PP
     * <p>-1 means we have neither
     */
    private int getPreambleType() {
        int preambleType;

        boolean hasTos = !TextUtils.isEmpty(mFlowParameters.termsOfServiceUrl);
        boolean hasPp = !TextUtils.isEmpty(mFlowParameters.privacyPolicyUrl);

        if (hasTos && hasPp) {
            preambleType = 0;
        } else if (hasTos) {
            preambleType = 1;
        } else if (hasPp) {
            preambleType = 2;
        } else {
            preambleType = -1;
        }

        return preambleType;
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
