package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.idp.ProviderHandler;
import com.firebase.ui.auth.viewmodel.idp.ProviderParamsBase;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandlerBase;
import com.google.firebase.auth.FacebookAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FacebookSignInHandler extends ProviderHandler<FacebookSignInHandler.Params>
        implements FacebookCallback<LoginResult> {
    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";

    private List<String> mPermissions;

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

    @Override
    protected void onCreate() {
        initPermissionList();
        LoginManager.getInstance().registerCallback(mCallbackManager, this);
    }

    private void initPermissionList() {
        List<String> scopes = getArguments().config.getParams()
                .getStringArrayList(ExtraConstants.EXTRA_FACEBOOK_PERMISSIONS);
        if (scopes == null) {
            scopes = new ArrayList<>();
        }

        List<String> permissionsList = new ArrayList<>(scopes);

        // Ensure we have email and public_profile scopes
        if (!permissionsList.contains(EMAIL)) {
            permissionsList.add(EMAIL);
        }
        if (!permissionsList.contains(PUBLIC_PROFILE)) {
            permissionsList.add(PUBLIC_PROFILE);
        }

        mPermissions = permissionsList;
    }

    public List<String> getPermissions() {
        return mPermissions;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
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
        setResult(IdpResponse.fromError(e));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        LoginManager.getInstance().unregisterCallback(mCallbackManager);
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
                setResult(IdpResponse.fromError(error == null ? new FirebaseUiException(
                        ErrorCodes.PROVIDER_ERROR, "Facebook graph request failed")
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

            setResult(createIdpResponse(mResult, email, name, photoUri));
        }
    }

    public static final class Params extends ProviderParamsBase {
        private final AuthUI.IdpConfig config;

        public Params(ProvidersHandlerBase handler, AuthUI.IdpConfig config) {
            super(handler);
            this.config = config;
        }
    }
}
