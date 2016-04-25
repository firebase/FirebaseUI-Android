package com.firebase.ui.auth.core;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.ui.R;
import com.firebase.ui.auth.google.GoogleAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class FirebaseLoginDialog extends DialogFragment {

    String TAG = "FirebaseLoginDialog";
    Map<AuthProviderType, FirebaseAuthProvider> mEnabledProvidersByType = new HashMap<>();
    TokenAuthHandler mHandler;
    AuthProviderType mActiveProvider;
    Firebase mRef;
    Context mContext;
    View mView;
    String mAction;

    @Override
    public void onStop() {
        super.onStop();
        cleanUp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanUp();
    }

    @Override
    public void onPause() {
        super.onPause();
       cleanUp();
    }

    public void cleanUp() {
        if (getGoogleAuthProvider() != null) getGoogleAuthProvider().cleanUp();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (FirebaseAuthProvider provider: mEnabledProvidersByType.values()) {
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Log.v(TAG, "CALLING onCreateDialog and inflating xml layout");

        if (mAction == "login") {
            mView = inflater.inflate(R.layout.fragment_firebase_login, null);
        } else if (mAction == "signup") {
            mView = inflater.inflate(R.layout.fragment_firebase_signup, null);
        }

        for (AuthProviderType providerType : AuthProviderType.values()) {
            if (mEnabledProvidersByType.keySet().contains(providerType)) {
                if (mAction == "login") {
                    showLoginOption(mEnabledProvidersByType.get(providerType), providerType.getButtonId());
                } else if (mAction == "signup") {
                    showSignupOption(mEnabledProvidersByType.get(providerType), providerType.getButtonId());
                }
            }
            else {
                mView.findViewById(providerType.getViewId()).setVisibility(View.GONE);
            }
        }

        if (mEnabledProvidersByType.containsKey(AuthProviderType.PASSWORD) &&
           !(mEnabledProvidersByType.containsKey(AuthProviderType.FACEBOOK) || mEnabledProvidersByType.containsKey(AuthProviderType.GOOGLE) || mEnabledProvidersByType.containsKey(AuthProviderType.TWITTER))) {
            mView.findViewById(R.id.or_section).setVisibility(View.GONE);
        }

        mView.findViewById(R.id.loading_section).setVisibility(View.GONE);
        builder.setView(mView);

        this.setRetainInstance(true);
        return builder.create();
    }

    public FirebaseLoginDialog setRef(Firebase ref) {
        mRef = ref;
        return this;
    }

    public FirebaseLoginDialog setContext(Context context) {
        mContext = context;
        return this;
    }

    public FirebaseLoginDialog setAction(String action) {
        Log.v(TAG, "CALLING setAction()");
        mAction = action;
        //adjust which xml layout to load, and some labels, clicklisteners
        return this;
    }

    public void reset() {
        if (mView != null) {
            if (mAction == "login") {
                mView.findViewById(R.id.login_section).setVisibility(View.VISIBLE);
            } else if (mAction == "signup") {
                mView.findViewById(R.id.signup_section).setVisibility(View.VISIBLE);
            }
            mView.findViewById(R.id.loading_section).setVisibility(View.GONE);
        }
    }

    public void logout() {
        for (FirebaseAuthProvider provider : mEnabledProvidersByType.values()) {
            provider.logout();
        }
        mRef.unauth();
    }

    public FirebaseLoginDialog setHandler(final TokenAuthHandler handler) {
        mHandler = new TokenAuthHandler() {
            @Override
            public void onSignupSuccess(Map<String, Object> result) {
                dismiss();
                handler.onSignupSuccess(result);
            }

            @Override
            public void onSignupUserError(FirebaseSignupError err) {
                handler.onSignupUserError(err);
            }

            @Override
            public void onLoginSuccess(AuthData auth) {
                dismiss();
                handler.onLoginSuccess(auth);
            }

            @Override
            public void onLoginUserError(FirebaseLoginError err) {
                handler.onLoginUserError(err);
            }

            @Override
            public void onProviderError(FirebaseLoginError err) {
                handler.onProviderError(err);
            }
        };
        return this;
    }

    public FirebaseLoginDialog setEnabledProvider(AuthProviderType provider) {
        if (!mEnabledProvidersByType.containsKey(provider)) {
            mEnabledProvidersByType.put(provider, provider.createProvider(mContext, mRef, mHandler));
        }
        return this;
    }

    private void showSignupOption(final FirebaseAuthProvider helper, int id) {
        mView.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AuthProviderType.getTypeForProvider(helper) == AuthProviderType.PASSWORD) {
                    EditText emailText = (EditText) mView.findViewById(R.id.email);
                    EditText passwordText = (EditText) mView.findViewById(R.id.password);
                    EditText passwordText2 = (EditText) mView.findViewById(R.id.password2);
                    helper.signup(emailText.getText().toString(), passwordText.getText().toString(), passwordText2.getText().toString());

                    passwordText.setText("");
                    passwordText2.setText("");
                } else {
                    helper.signup();
                }
                mActiveProvider = helper.getProviderType();
                mView.findViewById(R.id.signup_section).setVisibility(View.GONE);
                mView.findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
            }
        });
    }

    private void showLoginOption(final FirebaseAuthProvider helper, int id) {
        mView.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (AuthProviderType.getTypeForProvider(helper) == AuthProviderType.PASSWORD) {
                EditText emailText = (EditText) mView.findViewById(R.id.email);
                EditText passwordText = (EditText) mView.findViewById(R.id.password);
                helper.login(emailText.getText().toString(), passwordText.getText().toString());

                passwordText.setText("");
            } else {
                helper.login();
            }
            mActiveProvider = helper.getProviderType();
            mView.findViewById(R.id.login_section).setVisibility(View.GONE);
            mView.findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
            }
        });
    }

    public GoogleAuthProvider getGoogleAuthProvider() {
        return (GoogleAuthProvider) mEnabledProvidersByType.get(AuthProviderType.GOOGLE);
    }
}