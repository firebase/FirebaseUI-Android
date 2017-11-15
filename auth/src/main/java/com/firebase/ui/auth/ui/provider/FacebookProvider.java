package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.annotation.LayoutRes;

import com.facebook.WebDialog;
import com.facebook.login.LoginManager;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;

public class FacebookProvider implements Provider {
    private final FacebookSignInHandler mHandler;

    public FacebookProvider(HelperActivityBase activity, AuthUI.IdpConfig idpConfig) {
        WebDialog.setWebDialogTheme(activity.getFlowHolder().getParams().themeId);
        mHandler = ViewModelProviders.of(activity).get(FacebookSignInHandler.class);
        mHandler.init(new FacebookSignInHandler.Params(
                idpConfig, activity.getSignInHandler(), activity.getFlowHolder()));
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_idp_name_facebook);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_facebook;
    }

    @Override
    public void startLogin(HelperActivityBase activity) {
        activity.getFlowHolder().getProgressListener().setValue(false);
        LoginManager.getInstance().logInWithReadPermissions(activity, mHandler.getPermissions());
    }
}
