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

import java.lang.ref.WeakReference;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PreambleHandler {
    private static final String BTN_TARGET = "%BTN%";
    private static final String TOS_TARGET = "%TOS%";
    private static final String PP_TARGET = "%PP%";
    private static final int NO_BUTTON = -1;

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
                             @StringRes int textViewText,
                             TextView textView) {
        setup(context, parameters, NO_BUTTON, textViewText, textView);
    }

    public static void setup(Context context,
                             FlowParameters parameters,
                             @StringRes int buttonText,
                             @StringRes int textViewText,
                             TextView textView) {
        PreambleHandler handler = new PreambleHandler(context, parameters, buttonText);
        handler.initPreamble(textViewText);
        handler.setPreamble(textView);
    }

    private void setPreamble(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(mBuilder);
    }

    private void initPreamble(@StringRes int textViewText) {
        String withTargets = getPreambleStringWithTargets(textViewText, mButtonText != NO_BUTTON);
        if (withTargets == null) {
            return;
        }

        mBuilder = new SpannableStringBuilder(withTargets);

        replaceTarget(BTN_TARGET, mButtonText);
        replaceUrlTarget(TOS_TARGET, R.string.fui_terms_of_service, mFlowParameters.termsOfServiceUrl);
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
            mBuilder.setSpan(new CustomTabsSpan(mContext, url), targetIndex, end, 0);
        }
    }

    @Nullable
    private String getPreambleStringWithTargets(@StringRes int textViewText, boolean hasButton) {
        boolean termsOfServiceUrlProvided = !TextUtils.isEmpty(mFlowParameters.termsOfServiceUrl);
        boolean privacyPolicyUrlProvided = !TextUtils.isEmpty(mFlowParameters.privacyPolicyUrl);

        if (termsOfServiceUrlProvided && privacyPolicyUrlProvided) {
            Object[] targets = hasButton ?
                    new Object[]{BTN_TARGET, TOS_TARGET, PP_TARGET}
                    : new Object[]{TOS_TARGET, PP_TARGET};
            return mContext.getString(textViewText, targets);
        }

        return null;
    }

    private static final class CustomTabsSpan extends ClickableSpan {
        private final WeakReference<Context> mContext;
        private final String mUrl;
        private final CustomTabsIntent mCustomTabsIntent;

        public CustomTabsSpan(Context context, String url) {
            mContext = new WeakReference<>(context);
            mUrl = url;

            // Getting default color
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            @ColorInt int color = typedValue.data;

            mCustomTabsIntent = new CustomTabsIntent.Builder()
                    .setToolbarColor(color)
                    .setShowTitle(true)
                    .build();
        }

        @Override
        public void onClick(View widget) {
            Context context = mContext.get();
            if (context != null) {
                mCustomTabsIntent.launchUrl(context, Uri.parse(mUrl));
            }
        }
    }
}
