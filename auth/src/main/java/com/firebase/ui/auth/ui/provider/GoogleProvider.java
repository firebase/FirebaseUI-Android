package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;

public class GoogleProvider implements Provider {
    private final GoogleSignInHandler mHandler;

    public GoogleProvider(HelperActivityBase activity, IdpConfig idpConfig) {
        this(activity, idpConfig, null);
    }

    public GoogleProvider(HelperActivityBase activity,
                          IdpConfig idpConfig,
                          @Nullable String email) {
        mHandler = ViewModelProviders.of(activity).get(GoogleSignInHandler.class);
        mHandler.init(new GoogleSignInHandler.Params(
                idpConfig, activity.getSignInHandler(), activity.getFlowHolder(), email));
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_idp_name_google);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_google;
    }

    @Override
    public void startLogin(HelperActivityBase activity) {
        activity.getFlowHolder().getProgressListener().setValue(false);
        mHandler.start();
    }
}
