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
import com.firebase.client.FirebaseError;
import com.firebase.ui.R;
import com.firebase.ui.auth.facebook.FacebookAuthHelper;
import com.firebase.ui.auth.google.GoogleAuthHelper;
import com.firebase.ui.auth.password.PasswordAuthHelper;
import com.firebase.ui.auth.twitter.TwitterAuthHelper;

public class FirebaseLoginDialog extends DialogFragment {

    FacebookAuthHelper mFacebookAuthHelper;
    TwitterAuthHelper mTwitterAuthHelper;
    GoogleAuthHelper mGoogleAuthHelper;
    PasswordAuthHelper mPasswordAuthHelper;

    TokenAuthHandler mHandler;
    Firebase mRef;
    Context mContext;
    public Boolean isActive = false;

    public void onStart() {
        super.onStart();
        isActive = true;
    }

    public void onDestroy() {
        super.onDestroy();
        isActive = false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFacebookAuthHelper != null) {
            mFacebookAuthHelper.mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        if (mTwitterAuthHelper != null) {
            mTwitterAuthHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    View mView;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mView = inflater.inflate(R.layout.fragment_firebase_login, null);

        if (mFacebookAuthHelper != null) showLoginOption(mFacebookAuthHelper, R.id.facebook_button);
        else mView.findViewById(R.id.facebook_button).setVisibility(View.GONE);

        if (mGoogleAuthHelper != null) showLoginOption(mGoogleAuthHelper, R.id.google_button);
        else mView.findViewById(R.id.google_button).setVisibility(View.GONE);

        if (mTwitterAuthHelper != null) showLoginOption(mTwitterAuthHelper, R.id.twitter_button);
        else mView.findViewById(R.id.twitter_button).setVisibility(View.GONE);

        if (mPasswordAuthHelper != null) {
            showLoginOption(mPasswordAuthHelper, R.id.password_button);
            if (mFacebookAuthHelper == null && mGoogleAuthHelper == null && mTwitterAuthHelper == null)
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
            public void onUserError(FirebaseError err) {
                handler.onUserError(err);
            }

            @Override
            public void onProviderError(FirebaseError err) {
                handler.onProviderError(err);
            }
        };
        return this;
    }

    public FirebaseLoginDialog setContext(Context context) {
        mContext = context;
        return this;
    }

    public FirebaseLoginDialog setProviderEnabled(SocialProvider provider) {
        switch (provider) {
            case facebook:
                mFacebookAuthHelper = new FacebookAuthHelper(mContext, mRef, mHandler);
                break;
            case google:
                mGoogleAuthHelper = new GoogleAuthHelper(mContext, mRef, mHandler);
                break;
            case twitter:
                mTwitterAuthHelper = new TwitterAuthHelper(mContext, mRef, mHandler);
                break;
            case password:
                mPasswordAuthHelper = new PasswordAuthHelper(mContext, mRef, mHandler);
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
            } else {
                helper.login();
            }
            mView.findViewById(R.id.login_section).setVisibility(View.GONE);
            mView.findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
            }
        });

    }
}