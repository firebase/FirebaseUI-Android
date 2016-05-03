package com.firebase.ui.auth.choreographer.idp.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.firebase.ui.auth.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleProvider implements IDPProvider, OnClickListener {
    private static final int RC_SIGN_IN = 20;
    private static final String ERROR_KEY = "error";
    private static final String TOKEN_KEY = "token_key";
    private static final String CLIENT_ID_KEY = "client_id_key";
    private GoogleApiClient mGoogleApiClient;
    private Activity mActivity;
    private IDPCallback mIDPCallback;

    public GoogleProvider(Activity activity, IDPProviderParcel parcel) {
        mActivity = activity;
        String mClientId = parcel.getProviderExtra().getString(CLIENT_ID_KEY);
        GoogleSignInOptions googleSignInOptions;

        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(mClientId)
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
        mGoogleApiClient.connect();
    }

    public String getName(Context context) {
        return context.getResources().getString(R.string.idp_name_google);
    }

    @Override
    public View getLoginButton(Context context) {
        SignInButton signInButton = new SignInButton(context);
        signInButton.setOnClickListener(this);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        return signInButton;
    }

    public static IDPProviderParcel createParcel(String clientId) {
        Bundle extra = new Bundle();
        extra.putString(CLIENT_ID_KEY, clientId);
        return new IDPProviderParcel(GoogleAuthProvider.PROVIDER_ID, extra);
    }

    public static AuthCredential createAuthCredential(IDPResponse response) {
        Bundle bundle = response.getResponse();
        return GoogleAuthProvider.getCredential(bundle.getString(TOKEN_KEY), null);
    }

    @Override
    public void setAuthenticationCallback(IDPCallback callback) {
        mIDPCallback = callback;
    }

    private IDPResponse createIDPResponse(GoogleSignInAccount account) {
        Bundle response = new Bundle();
        response.putString(TOKEN_KEY, account.getIdToken());
        return new IDPResponse(GoogleAuthProvider.PROVIDER_ID, response);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result != null) {
                if (result.isSuccess()) {
                    mIDPCallback.onSuccess(createIDPResponse(result.getSignInAccount()));
                } else {
                    onError(result.getStatus().getStatusMessage());
                }
            } else {
                onError("No result found in intent");
            }
        }
    }

    @Override
    public void startLogin(Activity activity, String mEmail) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void onError(String errorMessage) {
        Bundle extra = new Bundle();
        extra.putString(ERROR_KEY, errorMessage);
        mIDPCallback.onFailure(extra);
    }

    @Override
    public void onClick(View view) {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        startLogin(mActivity, null);
    }
}

