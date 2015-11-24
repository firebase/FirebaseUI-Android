package com.firebase.ui.auth.core;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.ui.R;
import com.firebase.ui.auth.facebook.FacebookAuthProvider;
import com.firebase.ui.auth.google.GoogleAuthProvider;
import com.firebase.ui.auth.password.PasswordAuthProvider;
import com.firebase.ui.auth.twitter.TwitterAuthProvider;

public class FirebaseLoginDialog extends DialogFragment {

    FacebookAuthProvider mFacebookAuthProvider;
    TwitterAuthProvider mTwitterAuthProvider;
    GoogleAuthProvider mGoogleAuthProvider;
    PasswordAuthProvider mPasswordAuthProvider;
    TokenAuthHandler mHandler;
    Firebase mRef;
    Context mContext;
    View mView;

    public void onStart() {
        super.onStart();
        if (mGoogleAuthProvider != null) mGoogleAuthProvider.onStart();
    }

    public void onStop() {
        super.onStop();
        if (mGoogleAuthProvider != null) mGoogleAuthProvider.onStop();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mGoogleAuthProvider != null) mGoogleAuthProvider.onStop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFacebookAuthProvider != null) {
            mFacebookAuthProvider.mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        if (mTwitterAuthProvider != null) {
            mTwitterAuthProvider.onActivityResult(requestCode, resultCode, data);
        }

        if (mGoogleAuthProvider != null) {
            mGoogleAuthProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mView = inflater.inflate(R.layout.fragment_firebase_login, null);

        if (mFacebookAuthProvider != null) showLoginOption(mFacebookAuthProvider, R.id.facebook_button);
        else mView.findViewById(R.id.facebook_button).setVisibility(View.GONE);

        if (mGoogleAuthProvider != null) showLoginOption(mGoogleAuthProvider, R.id.google_button);
        else mView.findViewById(R.id.google_button).setVisibility(View.GONE);

        if (mTwitterAuthProvider != null) showLoginOption(mTwitterAuthProvider, R.id.twitter_button);
        else mView.findViewById(R.id.twitter_button).setVisibility(View.GONE);

        if (mPasswordAuthProvider != null) {
            showLoginOption(mPasswordAuthProvider, R.id.password_button);
            if (mFacebookAuthProvider == null && mGoogleAuthProvider == null && mTwitterAuthProvider == null)
                mView.findViewById(R.id.or_section).setVisibility(View.GONE);
        }
        else mView.findViewById(R.id.password_section).setVisibility(View.GONE);

        mView.findViewById(R.id.loading_section).setVisibility(View.GONE);

        builder.setView(mView);
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

    public void reset() {
        mView.findViewById(R.id.login_section).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.loading_section).setVisibility(View.GONE);
    }

    public void logout() {
        if (mFacebookAuthProvider != null) mFacebookAuthProvider.logout();
        if (mGoogleAuthProvider != null) mGoogleAuthProvider.logout();
        if (mTwitterAuthProvider != null) mTwitterAuthProvider.logout();
        if (mPasswordAuthProvider != null) mPasswordAuthProvider.logout();
        mRef.unauth();
    }

    public FirebaseLoginDialog setHandler(final TokenAuthHandler handler) {
        //TODO: Make this idiomatic?
        final DialogFragment self = this;
        mHandler = new TokenAuthHandler() {
            @Override
            public void onSuccess(AuthData auth) {
                self.dismiss();
                handler.onSuccess(auth);
            }

            @Override
            public void onUserError(FirebaseLoginError err) {
                handler.onUserError(err);
            }

            @Override
            public void onProviderError(FirebaseLoginError err) {
                handler.onProviderError(err);
            }
        };
        return this;
    }

    public FirebaseLoginDialog setProviderEnabled(SocialProvider provider) {
        switch (provider) {
            case facebook:
                if (mFacebookAuthProvider == null)
                    mFacebookAuthProvider = new FacebookAuthProvider(mContext, mRef, mHandler);
                break;
            case google:
                if (mGoogleAuthProvider == null)
                    mGoogleAuthProvider = new GoogleAuthProvider(mContext, mRef, mHandler);
                break;
            case twitter:
                if (mTwitterAuthProvider == null)
                    mTwitterAuthProvider = new TwitterAuthProvider(mContext, mRef, mHandler);
                break;
            case password:
                if (mPasswordAuthProvider == null)
                    mPasswordAuthProvider = new PasswordAuthProvider(mContext, mRef, mHandler);
                break;
        }

        return this;
    }

    private void showLoginOption(final FirebaseAuthHelper helper, int id) {
        mView.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (helper.getProviderName().equals("password")) {
                    EditText emailText = (EditText) mView.findViewById(R.id.email);
                    EditText passwordText = (EditText) mView.findViewById(R.id.password);
                    helper.login(emailText.getText().toString(), passwordText.getText().toString());

                    passwordText.setText("");
                } else {
                    helper.login();
                }
                mView.findViewById(R.id.login_section).setVisibility(View.GONE);
                mView.findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
            }
        });
    }
}