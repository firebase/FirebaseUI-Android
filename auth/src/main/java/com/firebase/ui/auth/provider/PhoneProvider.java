package com.firebase.ui.auth.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.firebase.auth.PhoneAuthProvider;

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
        AuthUI.IdpConfig config = ProviderUtils.getConfigFromIdps(
                mFlowParameters.providerInfo, PhoneAuthProvider.PROVIDER_ID);
        activity.startActivityForResult(
                PhoneActivity.createIntent(activity, mFlowParameters, config.getParams()),
                RC_PHONE_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PHONE_FLOW && resultCode == Activity.RESULT_OK) {
            mActivity.setResult(Activity.RESULT_OK, data);
            mActivity.finish();
        }
    }
}
