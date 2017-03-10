package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.ActivityHelper;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class AccountSettingsActivity extends UpEnabledActivity {
    private static final int RC_EDIT_DISPLAY_NAME = 100;
    private static final int RC_EDIT_EMAIL = 200;
    private static final int RC_CHANGE_PASSWORD = 300;
    private ImageView mProfilePicture;
    private TextView mDisplayName;
    private ImageButton mEditDisplayName;
    private TextView mEmail;
    private ImageButton mEditEmail;
    private Button mChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        final FlowParameters flowParams = mActivityHelper.getFlowParams();
        mActivityHelper.getCurrentUser().getPhotoUrl();
        mProfilePicture = (ImageView) findViewById(R.id.profile_image);
        Uri profilePhoto = mActivityHelper.getCurrentUser().getPhotoUrl();
        if (profilePhoto != null) {
            Picasso.with(this).load(profilePhoto).into(mProfilePicture);
        }
        mDisplayName = (TextView) findViewById(R.id.display_name);
        mEditDisplayName = (ImageButton) findViewById(R.id.edit_display_name);
        mEmail = (TextView) findViewById(R.id.email);
        mEditEmail = (ImageButton) findViewById(R.id.edit_email);

        mEditDisplayName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        EditDisplayNameActivity.createIntent(
                                getApplicationContext(),
                                flowParams),
                        RC_EDIT_DISPLAY_NAME);
            }
        });

        mEditEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        EditEmailActivity.createIntent(
                                getApplicationContext(),
                                flowParams),
                        RC_EDIT_EMAIL);
            }
        });

        mChangePassword = (Button) findViewById(R.id.change_password);
        mChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        EditPasswordActivity.createIntent(
                                getApplicationContext(),
                                flowParams),
                        RC_CHANGE_PASSWORD);
            }
        });

        populateView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_EDIT_DISPLAY_NAME:
            case RC_EDIT_EMAIL:
                populateView();
            case RC_CHANGE_PASSWORD:
            default:
                // fall through
        }
    }

    private void populateView() {
        FirebaseUser firebaseUser = mActivityHelper.getCurrentUser();
        mDisplayName.setText(firebaseUser.getDisplayName());
        mEmail.setText(firebaseUser.getEmail());
    }

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return BaseHelper.createBaseIntent(context, AccountSettingsActivity.class, flowParams);
    }
}
