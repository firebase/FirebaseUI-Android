package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.util.ui.FlowUtils;
import com.firebase.ui.auth.viewmodel.idp.ProviderResponseHandlerBase;
import com.google.firebase.auth.GoogleAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GoogleProvider extends ProviderBase {
    private final GoogleSignInHandler mHandler;

    public GoogleProvider(ProviderResponseHandlerBase handler, HelperActivityBase activity) {
        this(handler, activity, null);
    }

    public GoogleProvider(final ProviderResponseHandlerBase handler,
                          final HelperActivityBase activity,
                          @Nullable String email) {
        super(handler);
        mHandler = ViewModelProviders.of(activity).get(GoogleSignInHandler.class);
        mHandler.init(new GoogleSignInHandler.Params(
                handler,
                ProviderUtils.getConfigFromIdpsOrThrow(
                        activity.getFlowParams().providerInfo, GoogleAuthProvider.PROVIDER_ID),
                email));
        mHandler.getRequest().observe(activity, new Observer<Resource<Void>>() {
            @Override
            public void onChanged(Resource<Void> resource) {
                if (!resource.isUsed()) {
                    FlowUtils.handleError(activity, resource.getException());
                }
            }
        });
    }

    @StringRes
    @Override
    public int getNameRes() {
        return R.string.fui_idp_name_google;
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_google;
    }

    @Override
    public void startLogin(@NonNull HelperActivityBase activity) {
        mHandler.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mHandler.onActivityResult(requestCode, resultCode, data);
    }
}
