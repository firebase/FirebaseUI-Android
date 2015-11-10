package com.firebase.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.firebase.ui.auth.FirebaseAuthHelper;

public class FirebaseLoginDialog extends DialogFragment {

    FirebaseAuthHelper mFacebookAuthHelper;
    FirebaseAuthHelper mTwitterAuthHelper;
    FirebaseAuthHelper mGoogleAuthHelper;
    FirebaseAuthHelper mPasswordAuthHelper;
    View mView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
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

        builder.setView(mView);
        return builder.create();
    }

    public FirebaseLoginDialog addAuthHelper(FirebaseAuthHelper helper) {
        switch (helper.getProviderName()) {
            case "google":
                mGoogleAuthHelper = helper;
                break;
            case "facebook":
                mFacebookAuthHelper = helper;
                break;
            case "twitter":
                mTwitterAuthHelper = helper;
                break;
            case "password":
                mPasswordAuthHelper = helper;
        }

        return this;
    }

    private void showLoginOption(final FirebaseAuthHelper helper, int id) {
        mView.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (helper.getProviderName() == "password") {
                EditText emailText = (EditText) mView.findViewById(R.id.email);
                EditText passwordText = (EditText) mView.findViewById(R.id.password);
                helper.login(emailText.getText().toString(), passwordText.getText().toString());
            } else {
                helper.login();
            }
            }
        });
    }
}