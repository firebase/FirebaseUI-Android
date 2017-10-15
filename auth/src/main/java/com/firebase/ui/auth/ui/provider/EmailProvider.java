package com.firebase.ui.auth.ui.provider;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.email.EmailActivity;
import com.firebase.ui.auth.util.ui.ActivityResult;

public class EmailProvider implements Provider {
    private static final int RC_EMAIL_FLOW = 8;

    public EmailProvider(final HelperActivityBase activity) {
        activity.getFlowHolder()
                .getActivityResultListener()
                .observe(activity, new Observer<ActivityResult>() {
                    @Override
                    public void onChanged(@Nullable ActivityResult result) {
                        if (result.getRequestCode() == RC_EMAIL_FLOW
                                && result.getResultCode() == Activity.RESULT_OK) {
                            activity.finish(Activity.RESULT_OK, result.getData());
                        }
                    }
                });
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
    public void startLogin(HelperActivityBase activity) {
        activity.startActivityForResult(
                EmailActivity.createIntent(activity, activity.getFlowHolder().getParams()),
                RC_EMAIL_FLOW);
    }
}
