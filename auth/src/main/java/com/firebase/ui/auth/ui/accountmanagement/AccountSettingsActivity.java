package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AccountSettingsActivity extends UpEnabledActivity {
    public static final int RESULT_SIGNED_OUT = 2;

    private static final String TAG = "AccountSettingsAct";
    private static final int RC_EDIT_DISPLAY_NAME = 100;
    private static final int RC_EDIT_EMAIL = 200;
    private static final int RC_CHANGE_PASSWORD = 300;
    private ImageView mProfilePicture;
    private TextView mDisplayName;
    private ImageButton mEditDisplayName;
    private TextView mEmail;
    private ImageButton mEditEmail;
    private Button mChangePassword;
    private Button mSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        final FlowParameters flowParams = mActivityHelper.getFlowParams();
        mActivityHelper.getCurrentUser().getPhotoUrl();
        mSignOut = (Button) findViewById(R.id.sign_out_button);
        mSignOut.setOnClickListener(new SignOutClickListener());
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
        populateLinkedAccounts();
    }

    private void populateLinkedAccounts() {
        View linkedAccountsSection = findViewById(R.id.linked_accounts_section);
        FirebaseUser user = mActivityHelper.getCurrentUser();
        if (user == null) {
            return;
        }
        List<String> providers = user.getProviders();
        if (providers == null || providers.isEmpty()) {
            linkedAccountsSection.setVisibility(View.GONE);
            return;
        } else {
            linkedAccountsSection.setVisibility(View.VISIBLE);
        }
        ViewGroup linkedAccountHolder = (ViewGroup) findViewById(R.id.linked_accounts_holder);
        linkedAccountHolder.removeAllViews();
        LayoutInflater layoutInflater = getLayoutInflater();
        View row;
        for (String providerId : providers) {
            row = layoutInflater.inflate(R.layout.linked_provider, linkedAccountHolder);
            if (row != null) {
                ImageView providerImage = (ImageView) row.findViewById(R.id.provider_image);
                TextView providerName = (TextView) row.findViewById(R.id.provider_name);
                TextView displayName = (TextView) row.findViewById(R.id.display_name);
                displayName.setText(user.getDisplayName());
                ImageButton unlinkButton = (ImageButton) row.findViewById(R.id.unlink_idp_button);
                unlinkButton.setOnClickListener(new UnlinkIdpClickListener(providerId));
                switch (providerId) {
                    case AuthUI.EMAIL_PROVIDER:
                        providerImage.setImageResource(R.drawable.ic_email_white_48dp);
                        providerName.setText(R.string.idp_name_email);
                        break;
                    case AuthUI.FACEBOOK_PROVIDER:
                        providerImage.setImageResource(R.drawable.com_facebook_button_icon_blue);
                        providerName.setText(R.string.idp_name_facebook);
                        break;
                    case AuthUI.GOOGLE_PROVIDER:
                        providerImage.setImageResource(R.drawable.ic_googleg_color_24dp);
                        providerName.setText(R.string.idp_name_google);
                        break;
                    case AuthUI.TWITTER_PROVIDER:
                        providerImage.setImageResource(R.drawable.tw__composer_logo_blue);
                        providerName.setText(R.string.idp_name_twitter);
                        break;
                }

            }
        }
    }

    class UnlinkIdpClickListener implements View.OnClickListener {
        private String mProviderId;

        public UnlinkIdpClickListener(String providerId) {
            mProviderId = providerId;
        }

        @Override
        public void onClick(View v) {
            mActivityHelper
                    .getCurrentUser()
                    .unlink(mProviderId)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Failed to unlink " + mProviderId))
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult result) {
                            populateLinkedAccounts();
                        }
                    });
        }
    }

    class SignOutClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AuthUI.getInstance().signOut(AccountSettingsActivity.this);
            setResult(RESULT_SIGNED_OUT);
            finish();
        }
    }

    public static Intent createIntent(Context context, FlowParameters flowParams) {
        return BaseHelper.createBaseIntent(context, AccountSettingsActivity.class, flowParams);
    }
}
