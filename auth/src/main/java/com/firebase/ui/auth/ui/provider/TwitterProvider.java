package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TwitterProvider extends ProviderBase {
    private final TwitterSignInHandler mHandler;

    public TwitterProvider(ProvidersHandler handler, HelperActivityBase activity) {
        super(handler);
        mHandler = ViewModelProviders.of(activity).get(TwitterSignInHandler.class);
        mHandler.init(new TwitterSignInHandler.Params(handler));
    }

    @NonNull
    @Override
    public String getName() {
        return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_twitter);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_twitter;
    }

    @Override
    public void startLogin(@NonNull HelperActivityBase activity) {
        mHandler.getClient().authorize(activity, mHandler.getCallback());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mHandler.onActivityResult(requestCode, resultCode, data);
    }
}
