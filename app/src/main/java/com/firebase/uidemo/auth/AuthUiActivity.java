/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.uidemo.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.uidemo.R;
import com.firebase.uidemo.util.ConfigurationUtils;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AuthUiActivity extends AppCompatActivity {
    private static final String TAG = "AuthUiActivity";

    private static final String GOOGLE_TOS_URL = "https://www.google.com/policies/terms/";
    private static final String FIREBASE_TOS_URL = "https://firebase.google.com/terms/";
    private static final String GOOGLE_PRIVACY_POLICY_URL = "https://www.google.com/policies/privacy/";
    private static final String FIREBASE_PRIVACY_POLICY_URL = "https://firebase.google.com/terms/analytics/#7_privacy";

    private static final int RC_SIGN_IN = 100;

    @BindView(R.id.root) View mRootView;

    @BindView(R.id.google_provider) CheckBox mUseGoogleProvider;
    @BindView(R.id.facebook_provider) CheckBox mUseFacebookProvider;
    @BindView(R.id.twitter_provider) CheckBox mUseTwitterProvider;
    @BindView(R.id.github_provider) CheckBox mUseGitHubProvider;
    @BindView(R.id.email_provider) CheckBox mUseEmailProvider;
    @BindView(R.id.phone_provider) CheckBox mUsePhoneProvider;
    @BindView(R.id.anonymous_provider) CheckBox mUseAnonymousProvider;

    @BindView(R.id.default_theme) RadioButton mDefaultTheme;
    @BindView(R.id.green_theme) RadioButton mGreenTheme;
    @BindView(R.id.purple_theme) RadioButton mPurpleTheme;
    @BindView(R.id.dark_theme) RadioButton mDarkTheme;

    @BindView(R.id.firebase_logo) RadioButton mFirebaseLogo;
    @BindView(R.id.google_logo) RadioButton mGoogleLogo;
    @BindView(R.id.no_logo) RadioButton mNoLogo;

    @BindView(R.id.google_tos_privacy) RadioButton mUseGoogleTosPp;
    @BindView(R.id.firebase_tos_privacy) RadioButton mUseFirebaseTosPp;

    @BindView(R.id.google_scopes_header) TextView mGoogleScopesHeader;
    @BindView(R.id.google_scope_drive_file) CheckBox mGoogleScopeDriveFile;
    @BindView(R.id.google_scope_youtube_data) CheckBox mGoogleScopeYoutubeData;

    @BindView(R.id.facebook_permissions_header) TextView mFacebookPermissionsHeader;
    @BindView(R.id.facebook_permission_friends) CheckBox mFacebookPermissionFriends;
    @BindView(R.id.facebook_permission_photos) CheckBox mFacebookPermissionPhotos;

    @BindView(R.id.github_permissions_header) TextView mGitHubPermissionsHeader;
    @BindView(R.id.github_permission_repo) CheckBox mGitHubPermissionRepo;
    @BindView(R.id.github_permission_gist) CheckBox mGitHubPermissionGist;

    @BindView(R.id.credential_selector_enabled) CheckBox mEnableCredentialSelector;
    @BindView(R.id.hint_selector_enabled) CheckBox mEnableHintSelector;
    @BindView(R.id.allow_new_email_accounts) CheckBox mAllowNewEmailAccounts;
    @BindView(R.id.require_name) CheckBox mRequireName;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, AuthUiActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_ui_layout);
        ButterKnife.bind(this);

        if (ConfigurationUtils.isGoogleMisconfigured(this)) {
            mUseGoogleProvider.setChecked(false);
            mUseGoogleProvider.setEnabled(false);
            mUseGoogleProvider.setText(R.string.google_label_missing_config);
            setGoogleScopesEnabled(false);
        } else {
            setGoogleScopesEnabled(mUseGoogleProvider.isChecked());
            mUseGoogleProvider.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    setGoogleScopesEnabled(checked);
                }
            });
        }

        if (ConfigurationUtils.isFacebookMisconfigured(this)) {
            mUseFacebookProvider.setChecked(false);
            mUseFacebookProvider.setEnabled(false);
            mUseFacebookProvider.setText(R.string.facebook_label_missing_config);
            setFacebookPermissionsEnabled(false);
        } else {
            setFacebookPermissionsEnabled(mUseFacebookProvider.isChecked());
            mUseFacebookProvider.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    setFacebookPermissionsEnabled(checked);
                }
            });
        }

        if (ConfigurationUtils.isTwitterMisconfigured(this)) {
            mUseTwitterProvider.setChecked(false);
            mUseTwitterProvider.setEnabled(false);
            mUseTwitterProvider.setText(R.string.twitter_label_missing_config);
        }

        if (ConfigurationUtils.isGitHubMisconfigured(this)) {
            mUseGitHubProvider.setChecked(false);
            mUseGitHubProvider.setEnabled(false);
            mUseGitHubProvider.setText(R.string.github_label_missing_config);
            setGitHubPermissionsEnabled(false);
        } else {
            setGitHubPermissionsEnabled(mUseGitHubProvider.isChecked());
            mUseGitHubProvider.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    setGitHubPermissionsEnabled(checked);
                }
            });
        }

        if (ConfigurationUtils.isGoogleMisconfigured(this)
                || ConfigurationUtils.isFacebookMisconfigured(this)
                || ConfigurationUtils.isTwitterMisconfigured(this)
                || ConfigurationUtils.isGitHubMisconfigured(this)) {
            showSnackbar(R.string.configuration_required);
        }

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            mDarkTheme.setChecked(true);
        }
    }

    @OnClick(R.id.sign_in)
    public void signIn() {
        AuthUI.SignInIntentBuilder builder =  AuthUI.getInstance().createSignInIntentBuilder()
                .setTheme(getSelectedTheme())
                .setLogo(getSelectedLogo())
                .setAvailableProviders(getSelectedProviders())
                .setIsSmartLockEnabled(mEnableCredentialSelector.isChecked(),
                        mEnableHintSelector.isChecked());

        if (getSelectedTosUrl() != null && getSelectedPrivacyPolicyUrl() != null) {
            builder.setTosAndPrivacyPolicyUrls(
                    getSelectedTosUrl(),
                    getSelectedPrivacyPolicyUrl());
        }

        startActivityForResult(builder.build(), RC_SIGN_IN);
    }

    @OnClick(R.id.sign_in_silent)
    public void silentSignIn() {
        AuthUI.getInstance().silentSignIn(this, getSelectedProviders())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            startSignedInActivity(null);
                        } else {
                            showSnackbar(R.string.sign_in_failed);
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startSignedInActivity(null);
            finish();
        }
    }

    private void handleSignInResponse(int resultCode, @Nullable Intent data) {
        IdpResponse response = IdpResponse.fromResultIntent(data);

        // Successfully signed in
        if (resultCode == RESULT_OK) {
            startSignedInActivity(response);
            finish();
        } else {
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackbar(R.string.sign_in_cancelled);
                return;
            }

            if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(R.string.no_internet_connection);
                return;
            }

            showSnackbar(R.string.unknown_error);
            Log.e(TAG, "Sign-in error: ", response.getError());
        }
    }

    private void startSignedInActivity(@Nullable IdpResponse response) {
        startActivity(SignedInActivity.createIntent(this, response));
    }

    @OnClick({R.id.default_theme, R.id.purple_theme, R.id.green_theme, R.id.dark_theme})
    public void toggleDarkTheme() {
        int mode = mDarkTheme.isChecked() ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_AUTO;
        AppCompatDelegate.setDefaultNightMode(mode);
        getDelegate().setLocalNightMode(mode);
    }

    @StyleRes
    private int getSelectedTheme() {
        if (mGreenTheme.isChecked()) {
            return R.style.GreenTheme;
        }

        if (mPurpleTheme.isChecked()) {
            return R.style.PurpleTheme;
        }

        return AuthUI.getDefaultTheme();
    }

    @DrawableRes
    private int getSelectedLogo() {
        if (mFirebaseLogo.isChecked()) {
            return R.drawable.firebase_auth_120dp;
        } else if (mGoogleLogo.isChecked()) {
            return R.drawable.ic_googleg_color_144dp;
        }
        return AuthUI.NO_LOGO;
    }

    private List<IdpConfig> getSelectedProviders() {
        List<IdpConfig> selectedProviders = new ArrayList<>();

        if (mUseGoogleProvider.isChecked()) {
            selectedProviders.add(
                    new IdpConfig.GoogleBuilder().setScopes(getGoogleScopes()).build());
        }

        if (mUseFacebookProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.FacebookBuilder()
                    .setPermissions(getFacebookPermissions())
                    .build());
        }

        if (mUseTwitterProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.TwitterBuilder().build());
        }

        if (mUseGitHubProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.GitHubBuilder()
                    .setPermissions(getGitHubPermissions())
                    .build());
        }

        if (mUseEmailProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.EmailBuilder()
                    .setRequireName(mRequireName.isChecked())
                    .setAllowNewAccounts(mAllowNewEmailAccounts.isChecked())
                    .build());
        }

        if (mUsePhoneProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.PhoneBuilder().build());
        }

        if (mUseAnonymousProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.AnonymousBuilder().build());
        }

        return selectedProviders;
    }

    @Nullable
    private String getSelectedTosUrl() {
        if (mUseGoogleTosPp.isChecked()) {
            return GOOGLE_TOS_URL;
        }

        if (mUseFirebaseTosPp.isChecked()) {
            return FIREBASE_TOS_URL;
        }

        return null;
    }

    @Nullable
    private String getSelectedPrivacyPolicyUrl() {
        if (mUseGoogleTosPp.isChecked()) {
            return GOOGLE_PRIVACY_POLICY_URL;
        }

        if (mUseFirebaseTosPp.isChecked()) {
            return FIREBASE_PRIVACY_POLICY_URL;
        }

        return null;
    }

    private void setGoogleScopesEnabled(boolean enabled) {
        mGoogleScopesHeader.setEnabled(enabled);
        mGoogleScopeDriveFile.setEnabled(enabled);
        mGoogleScopeYoutubeData.setEnabled(enabled);
    }

    private void setFacebookPermissionsEnabled(boolean enabled) {
        mFacebookPermissionsHeader.setEnabled(enabled);
        mFacebookPermissionFriends.setEnabled(enabled);
        mFacebookPermissionPhotos.setEnabled(enabled);
    }

    private void setGitHubPermissionsEnabled(boolean enabled) {
        mGitHubPermissionsHeader.setEnabled(enabled);
        mGitHubPermissionRepo.setEnabled(enabled);
        mGitHubPermissionGist.setEnabled(enabled);
    }

    private List<String> getGoogleScopes() {
        List<String> result = new ArrayList<>();
        if (mGoogleScopeYoutubeData.isChecked()) {
            result.add("https://www.googleapis.com/auth/youtube.readonly");
        }
        if (mGoogleScopeDriveFile.isChecked()) {
            result.add(Scopes.DRIVE_FILE);
        }
        return result;
    }

    private List<String> getFacebookPermissions() {
        List<String> result = new ArrayList<>();
        if (mFacebookPermissionFriends.isChecked()) {
            result.add("user_friends");
        }
        if (mFacebookPermissionPhotos.isChecked()) {
            result.add("user_photos");
        }
        return result;
    }

    private List<String> getGitHubPermissions() {
        List<String> result = new ArrayList<>();
        if (mGitHubPermissionRepo.isChecked()) {
            result.add("repo");
        }
        if (mGitHubPermissionGist.isChecked()) {
            result.add("gist");
        }
        return result;
    }

    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
