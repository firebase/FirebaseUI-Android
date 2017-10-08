package com.firebase.ui.auth.ui.provider;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.phone.PhoneVerificationActivity;
import com.firebase.ui.auth.util.ui.ActivityResult;

public class PhoneProvider implements Provider {
    private static final int RC_PHONE_FLOW = 4;

    private final AuthUI.IdpConfig mConfig;

    public PhoneProvider(final HelperActivityBase activity, AuthUI.IdpConfig config) {
        mConfig = config;

        activity.getFlowHolder()
                .getActivityResultListener()
                .observe(activity, new Observer<ActivityResult>() {
                    @Override
                    public void onChanged(@Nullable ActivityResult result) {
                        if (result.getRequestCode() == RC_PHONE_FLOW
                                && result.getResultCode() == Activity.RESULT_OK) {
                            activity.finish(Activity.RESULT_OK, result.getData());
                        }
                    }
                });
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
    public void startLogin(HelperActivityBase activity) {
        activity.startActivityForResult(
                PhoneVerificationActivity.createIntent(
                        activity, activity.getFlowHolder().getParams(), mConfig.getParams()),
                RC_PHONE_FLOW);
    }
}
