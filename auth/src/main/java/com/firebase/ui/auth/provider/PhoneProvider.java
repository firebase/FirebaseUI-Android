package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.phone.PhoneVerificationActivity;

public class PhoneProvider implements Provider {

    private static final int RC_PHONE_FLOW = 4;

    private Activity mActivity;
    private FlowParameters mFlowParameters;

    public PhoneProvider(Activity activity, FlowParameters parameters) {
        mActivity = activity;
        mFlowParameters = parameters;
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_provider_name_phone);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_provider_button_phone;
    }

    @Override
    public void startLogin(Activity activity) {
        activity.startActivityForResult(
                PhoneVerificationActivity.createIntent(activity, mFlowParameters, null),
                RC_PHONE_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PHONE_FLOW && resultCode == ResultCodes.OK) {
            mActivity.setResult(ResultCodes.OK, data);
            mActivity.finish();
        }
    }
}
