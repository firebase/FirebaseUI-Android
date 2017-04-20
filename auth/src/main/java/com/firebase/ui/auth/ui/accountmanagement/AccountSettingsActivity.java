package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class AccountSettingsActivity extends UpEnabledActivity {
    public static final int RESULT_SIGNED_OUT = 2;

    private static final String TAG = "AccountSettingsAct";
    private static final int RC_EDIT_DISPLAY_NAME = 100;
    private static final int RC_EDIT_EMAIL = 200;
    private static final int RC_CHANGE_PASSWORD = 300;
    private static final int RC_REAUTH_EDIT_EMAIL = 400;
    private static final int RC_REAUTH_PASSWORD = 500;

    private TextView mDisplayName;
    private TextView mEmail;
    private FlowParameters mFlowParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        mFlowParameters = mActivityHelper.getFlowParams();
        Button signOut = (Button) findViewById(R.id.sign_out_button);
        signOut.setOnClickListener(new SignOutClickListener());
        ImageView profilePicture = (ImageView) findViewById(R.id.profile_image);

        Uri profilePhoto = mActivityHelper.getCurrentUser().getPhotoUrl();
        if (profilePhoto != null) {
            Glide.with(this)
                    .load(profilePhoto)
                    .placeholder(R.drawable.ic_person_white_48dp)
                    .transform(new CircleTransform(getApplicationContext()))
                    .into(profilePicture);
        }

        mDisplayName = (TextView) findViewById(R.id.display_name);
        ImageButton editDisplayName = (ImageButton) findViewById(R.id.edit_display_name);
        mEmail = (TextView) findViewById(R.id.email);
        ImageButton editEmail = (ImageButton) findViewById(R.id.edit_email);

        editDisplayName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        EditDisplayNameActivity.createIntent(
                                getApplicationContext(),
                                mFlowParameters),
                        RC_EDIT_DISPLAY_NAME);
            }
        });

        editEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(getReauthIntent(), RC_REAUTH_EDIT_EMAIL);
            }
        });

        Button changePassword = (Button) findViewById(R.id.change_password);
        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(getReauthIntent(), RC_REAUTH_PASSWORD);
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
                break;
            case RC_REAUTH_EDIT_EMAIL:
                switch (resultCode) {
                    case RESULT_OK:
                        startActivityForResult(
                                EditEmailActivity.createIntent(
                                        getApplicationContext(),
                                        mFlowParameters),
                                RC_EDIT_EMAIL);
                        break;
                    default:
                        // fall through
                }
                break;
            case RC_REAUTH_PASSWORD:
                switch (resultCode) {
                    case RESULT_OK:
                        startActivityForResult(
                                EditPasswordActivity.createIntent(this, mFlowParameters),
                                RC_CHANGE_PASSWORD);
                        break;
                    default:
                        // fall through
                }
                break;
            case RC_CHANGE_PASSWORD:
            default:
                // fall through
        }
    }

    private void populateView() {
        FirebaseUser firebaseUser = mActivityHelper.getCurrentUser();
        if (firebaseUser == null) {
            return;
        }
        mDisplayName.setText(firebaseUser.getDisplayName());
        mEmail.setText(firebaseUser.getEmail());
        populateLinkedAccounts();
        maybeShowChangePassword();
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
                ProviderView providerView = (ProviderView) row.findViewById(R.id.provider);
                providerView.setUnlinkListener(new UnlinkIdpClickListener(providerId));
                providerView.setDisplayName(user.getDisplayName());
                providerView.setProviderId(providerId);
            }
        }
    }

    private Intent getReauthIntent() {
        return AuthUI.getInstance().createReauthIntentBuilder()
                .setTosUrl(mFlowParameters.termsOfServiceUrl)
                .setLogo(mFlowParameters.logoId)
                .setProviders(mFlowParameters.providerInfo)
                .setIsSmartLockEnabled(mFlowParameters.smartLockEnabled)
                .build();
    }

    private void maybeShowChangePassword() {
        View changePasswordLayout = findViewById(R.id.change_password_layout);
        List<String> providers = mActivityHelper.getCurrentUser().getProviders();
        if (providers.isEmpty() || providers.contains(AuthUI.EMAIL_PROVIDER)) {
            changePasswordLayout.setVisibility(View.VISIBLE);
        } else {
            changePasswordLayout.setVisibility(View.GONE);
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
                    .addOnSuccessListener(
                            AccountSettingsActivity.this, new OnSuccessListener<AuthResult>() {
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
