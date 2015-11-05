package com.firebase.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.hardware.camera2.params.Face;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.firebase.ui.com.firebasei.ui.authimpl.FacebookAuthHelper;
import com.firebase.ui.com.firebasei.ui.authimpl.FirebaseAuthHelper;
import com.firebase.ui.com.firebasei.ui.authimpl.GoogleAuthHelper;
import com.firebase.ui.com.firebasei.ui.authimpl.SocialProvider;
import com.firebase.ui.com.firebasei.ui.authimpl.TwitterAuthHelper;

public class FirebaseLoginDialog extends DialogFragment {

    FirebaseAuthHelper mFacebookAuthHelper;
    FirebaseAuthHelper mTwitterAuthHelper;
    FirebaseAuthHelper mGoogleAuthHelper;
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
        }

        return this;
    }

    private void showLoginOption(final FirebaseAuthHelper helper, int id) {
        mView.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.login();
            }
        });
    }
}