package com.firebase.ui.auth.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.auth.util.smartlock.SignInDelegate;

public class KickoffActivity extends AppCompatBase {
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        SignInDelegate.delegate(this, mActivityHelper.getFlowParams());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SignInDelegate delegate = SignInDelegate.getInstance(this);
        if (delegate != null) {
            delegate.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return ActivityHelper.createBaseIntent(context, KickoffActivity.class, flowParams);
    }
}
