package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.email.EmailActivity;

public class EmailProvider implements Provider {
    private static final int RC_EMAIL_FLOW = 2;

    private Activity mActivity;
    private FlowParameters mFlowParameters;

    public EmailProvider(Activity activity, FlowParameters flowParameters) {
        mActivity = activity;
        mFlowParameters = flowParameters;
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_provider_name_email);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_provider_button_email;
    }

    @Override
    public void startLogin(Activity activity) {
        activity.startActivityForResult(
                EmailActivity.createIntent(activity, mFlowParameters),
                RC_EMAIL_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_EMAIL_FLOW && resultCode == Activity.RESULT_OK) {
            mActivity.setResult(Activity.RESULT_OK, data);
            mActivity.finish();
        }
    }
}
