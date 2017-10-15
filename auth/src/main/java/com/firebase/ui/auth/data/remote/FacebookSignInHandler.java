package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.arch.lifecycle.Observer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.ActivityResult;
import com.firebase.ui.auth.data.model.ProviderErrorException;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.firebase.ui.auth.util.ui.ViewModelBase;
import com.google.firebase.auth.FacebookAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FacebookSignInHandler extends ViewModelBase<FacebookSignInHandler.Params>
        implements Observer<ActivityResult>, FacebookCallback<LoginResult> {
    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";

    private AuthUI.IdpConfig mConfig;
    private SignInHandler mHandler;
    private FlowHolder mFlowHolder;

    private final CallbackManager mCallbackManager = CallbackManager.Factory.create();

    public FacebookSignInHandler(Application application) {
        super(application);
    }

    private static IdpResponse createIdpResponse(
            LoginResult result, @Nullable String email, String name, Uri photoUri) {
        return new IdpResponse.Builder(
                new User.Builder(FacebookAuthProvider.PROVIDER_ID, email)
                        .setName(name)
                        .setPhotoUri(photoUri)
                        .build())
                .setToken(result.getAccessToken().getToken())
                .build();
    }

    public List<String> getPermissions() {
        List<String> permissionsList = new ArrayList<>(mConfig.getScopes());

        // Ensure we have email and public_profile scopes
        if (!permissionsList.contains(EMAIL)) {
            permissionsList.add(EMAIL);
        }
        if (!permissionsList.contains(PUBLIC_PROFILE)) {
            permissionsList.add(PUBLIC_PROFILE);
        }

        return permissionsList;
    }

    @Override
    protected void onCreate(Params params) {
        mConfig = params.providerConfig;
        mHandler = params.signInHandler;
        mFlowHolder = params.flowHolder;

        mFlowHolder.getActivityResultListener().observeForever(this);
        LoginManager.getInstance().registerCallback(mCallbackManager, this);
    }

    @Override
    public void onChanged(@Nullable ActivityResult result) {
        mCallbackManager.onActivityResult(
                result.getRequestCode(), result.getResultCode(), result.getData());
    }

    @Override
    public void onSuccess(final LoginResult result) {
        GraphRequest request = GraphRequest.newMeRequest(result.getAccessToken(),
                new ProfileRequest(result));

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,picture");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    public void onCancel() {
        onError(new FacebookException("Sign in request cancelled."));
    }

    @Override
    public void onError(FacebookException e) {
        mHandler.start(IdpResponse.fromError(e));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFlowHolder.getActivityResultListener().removeObserver(this);
        LoginManager.getInstance().unregisterCallback(mCallbackManager);
    }

    public static final class Params {
        public final AuthUI.IdpConfig providerConfig;
        public final SignInHandler signInHandler;
        public final FlowHolder flowHolder;

        public Params(AuthUI.IdpConfig config, SignInHandler handler, FlowHolder holder) {
            providerConfig = config;
            signInHandler = handler;
            flowHolder = holder;
        }
    }

    private class ProfileRequest implements GraphRequest.GraphJSONObjectCallback {
        private final LoginResult mResult;

        public ProfileRequest(LoginResult result) {
            mResult = result;
        }

        @Override
        public void onCompleted(JSONObject object, GraphResponse response) {
            FacebookRequestError error = response.getError();
            if (error != null || object == null) {
                mHandler.start(IdpResponse.fromError(error == null ?
                        new ProviderErrorException("Facebook graph request failed")
                        : error.getException()));
                return;
            }

            String email = null;
            String name = null;
            Uri photoUri = null;

            try {
                email = object.getString("email");
            } catch (JSONException ignored) {}
            try {
                name = object.getString("name");
            } catch (JSONException ignored) {}
            try {
                photoUri = Uri.parse(object.getJSONObject("picture")
                        .getJSONObject("data")
                        .getString("url"));
            } catch (JSONException ignored) {}

            mHandler.start(createIdpResponse(mResult, email, name, photoUri));
        }
    }
}
