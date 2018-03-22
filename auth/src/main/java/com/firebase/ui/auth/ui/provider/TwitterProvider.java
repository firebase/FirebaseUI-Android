package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TwitterProvider implements Provider {
    private final TwitterSignInHandler mHandler;

    public TwitterProvider(HelperActivityBase activity) {
        mHandler = ViewModelProviders.of(activity).get(TwitterSignInHandler.class);
        mHandler.init(null);
    }

    @Override
    public LiveData<IdpResponse> getResponseListener() {
        return mHandler.getOperation();
    }

    @StringRes
    @Override
    public int getNameRes() {
        return R.string.fui_idp_name_twitter;
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_twitter;
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        mHandler.getClient().authorize(activity, mHandler.getCallback());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mHandler.onActivityResult(requestCode, resultCode, data);
    }
}
