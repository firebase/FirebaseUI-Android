package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.remote.GoogleParams;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleProvider extends ProviderBase {
    private final GoogleSignInHandler mHandler;

    public GoogleProvider(ProvidersHandler handler, HelperActivityBase activity) {
        this(handler, activity, null);
    }

    public GoogleProvider(final ProvidersHandler handler,
                          final HelperActivityBase activity,
                          @Nullable String email) {
        super(handler);
        mHandler = ViewModelProviders.of(activity).get(GoogleSignInHandler.class);
        mHandler.init(new GoogleParams(
                handler,
                ProviderUtils.getConfigFromIdpsOrThrow(
                        activity.getFlowParams().providerInfo, GoogleAuthProvider.PROVIDER_ID),
                email));
        mHandler.getRequest().observe(activity, new Observer<Resource<Intent>>() {
            @Override
            public void onChanged(Resource<Intent> resource) {
                if (!resource.isUsed()) {
                    activity.startActivityForResult(
                            resource.getValue(), RequestCodes.GOOGLE_PROVIDER);
                }
            }
        });
    }

    @NonNull
    @Override
    public String getName() {
        return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_google);
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
        if (requestCode == RequestCodes.GOOGLE_PROVIDER) {
            mHandler.onActivityResult(requestCode, resultCode, data);
        }
    }
}
