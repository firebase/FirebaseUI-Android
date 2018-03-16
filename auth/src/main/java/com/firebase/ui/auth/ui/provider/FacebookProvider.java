package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.facebook.WebDialog;
import com.facebook.login.LoginManager;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandler;
import com.google.firebase.auth.FacebookAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FacebookProvider extends ProviderBase {
    private final FacebookSignInHandler mHandler;

    public FacebookProvider(ProvidersHandler handler, HelperActivityBase activity) {
        super(handler);
        WebDialog.setWebDialogTheme(activity.getFlowParams().themeId);
        mHandler = ViewModelProviders.of(activity).get(FacebookSignInHandler.class);
        mHandler.init(new FacebookSignInHandler.Params(
                handler,
                ProviderUtils.getConfigFromIdpsOrThrow(
                        activity.getFlowParams().providerInfo, FacebookAuthProvider.PROVIDER_ID)));
    }

    @NonNull
    @Override
    public String getName() {
        return AuthUI.getApplicationContext().getString(R.string.fui_idp_name_facebook);
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_idp_button_facebook;
    }

    @Override
    public void startLogin(@NonNull HelperActivityBase activity) {
        LoginManager.getInstance().logInWithReadPermissions(activity, mHandler.getPermissions());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mHandler.onActivityResult(requestCode, resultCode, data);
    }
}
