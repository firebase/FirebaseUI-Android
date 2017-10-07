package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.annotation.LayoutRes;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;

public class TwitterProvider implements Provider {
    private final TwitterSignInHandler mHandler;

    public TwitterProvider(HelperActivityBase activity) {
        mHandler = ViewModelProviders.of(activity).get(TwitterSignInHandler.class);
        mHandler.init(new TwitterSignInHandler.Params(
                activity.getSignInHandler(), activity.getFlowHolder()));
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.fui_idp_name_twitter);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_twitter;
    }

    @Override
    public void startLogin(HelperActivityBase activity) {
        mHandler.getClient().authorize(activity, mHandler.getCallback());
    }
}
