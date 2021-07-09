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
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.uidemo.R;
import com.firebase.uidemo.databinding.AuthUiLayoutBinding;
import com.firebase.uidemo.util.ConfigurationUtils;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class AuthUiActivity extends AppCompatActivity
        implements ActivityResultCallback<FirebaseAuthUIAuthenticationResult> {
    private static final String TAG = "AuthUiActivity";

    private static final String GOOGLE_TOS_URL = "https://www.google.com/policies/terms/";
    private static final String FIREBASE_TOS_URL = "https://firebase.google.com/terms/";
    private static final String GOOGLE_PRIVACY_POLICY_URL = "https://www.google" +
            ".com/policies/privacy/";
    private static final String FIREBASE_PRIVACY_POLICY_URL = "https://firebase.google" +
            ".com/terms/analytics/#7_privacy";

    private static final int RC_SIGN_IN = 100;

    private AuthUiLayoutBinding mBinding;

    private final ActivityResultLauncher<Intent> signIn =
            registerForActivityResult(new FirebaseAuthUIActivityResultContract(), this);

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, AuthUiActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = AuthUiLayoutBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Workaround for vector drawables on API 19
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        if (ConfigurationUtils.isGoogleMisconfigured(this)) {
            mBinding.googleProvider.setChecked(false);
            mBinding.googleProvider.setEnabled(false);
            mBinding.googleProvider.setText(R.string.google_label_missing_config);
            setGoogleScopesEnabled(false);
        } else {
            setGoogleScopesEnabled(mBinding.googleProvider.isChecked());
            mBinding.googleProvider.setOnCheckedChangeListener((compoundButton, checked) -> setGoogleScopesEnabled(checked));
        }

        if (ConfigurationUtils.isFacebookMisconfigured(this)) {
            mBinding.facebookProvider.setChecked(false);
            mBinding.facebookProvider.setEnabled(false);
            mBinding.facebookProvider.setText(R.string.facebook_label_missing_config);
            setFacebookPermissionsEnabled(false);
        } else {
            setFacebookPermissionsEnabled(mBinding.facebookProvider.isChecked());
            mBinding.facebookProvider.setOnCheckedChangeListener((compoundButton, checked) -> setFacebookPermissionsEnabled(checked));
        }

        mBinding.emailLinkProvider.setOnCheckedChangeListener((buttonView, isChecked) -> flipPasswordProviderCheckbox(isChecked));

        mBinding.emailProvider.setOnCheckedChangeListener((buttonView, isChecked) -> flipEmailLinkProviderCheckbox(isChecked));

        mBinding.emailLinkProvider.setChecked(false);
        mBinding.emailProvider.setChecked(true);

        // The custom layout in this app only supports Email and Google providers.
        mBinding.customLayout.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                mBinding.googleProvider.setChecked(true);
                mBinding.emailProvider.setChecked(true);

                mBinding.facebookProvider.setChecked(false);
                mBinding.twitterProvider.setChecked(false);
                mBinding.emailLinkProvider.setChecked(false);
                mBinding.phoneProvider.setChecked(false);
                mBinding.anonymousProvider.setChecked(false);
                mBinding.microsoftProvider.setChecked(false);
                mBinding.yahooProvider.setChecked(false);
                mBinding.appleProvider.setChecked(false);
                mBinding.githubProvider.setChecked(false);
            }
        });

        // useEmulator can't be reversed until the FirebaseApp is cleared, so we make this
        // checkbox "sticky" until the app is restarted
        mBinding.useAuthEmulator.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mBinding.useAuthEmulator.setEnabled(false);
            }
        });

        mBinding.signIn.setOnClickListener(view -> signIn());

        mBinding.signInSilent.setOnClickListener(view -> silentSignIn());

        if (ConfigurationUtils.isGoogleMisconfigured(this)
                || ConfigurationUtils.isFacebookMisconfigured(this)) {
            showSnackbar(R.string.configuration_required);
        }

        catchEmailLinkSignIn();
    }

    public void catchEmailLinkSignIn() {
        if (getIntent().getExtras() == null) {
            return;
        }
        String link = getIntent().getExtras().getString(ExtraConstants.EMAIL_LINK_SIGN_IN);
        if (link != null) {
            signInWithEmailLink(link);
        }
    }

    public void flipPasswordProviderCheckbox(boolean emailLinkProviderIsChecked) {
        if (emailLinkProviderIsChecked) {
            mBinding.emailProvider.setChecked(false);
        }
    }

    public void flipEmailLinkProviderCheckbox(boolean passwordProviderIsChecked) {
        if (passwordProviderIsChecked) {
            mBinding.emailLinkProvider.setChecked(false);
        }
    }

    public void signIn() {
        signIn.launch(getSignInIntent(/*link=*/null));
    }

    public void signInWithEmailLink(@Nullable String link) {
        signIn.launch(getSignInIntent(link));
    }

    @NonNull
    public AuthUI getAuthUI() {
        AuthUI authUI = AuthUI.getInstance();
        if (mBinding.useAuthEmulator.isChecked()) {
            authUI.useEmulator("10.0.2.2", 9099);
        }

        return authUI;
    }

    private Intent getSignInIntent(@Nullable String link) {
        AuthUI.SignInIntentBuilder builder = getAuthUI().createSignInIntentBuilder()
                .setTheme(getSelectedTheme())
                .setLogo(getSelectedLogo())
                .setAvailableProviders(getSelectedProviders())
                .setIsSmartLockEnabled(mBinding.credentialSelectorEnabled.isChecked(),
                        mBinding.hintSelectorEnabled.isChecked());

        if (mBinding.customLayout.isChecked()) {
            AuthMethodPickerLayout customLayout = new AuthMethodPickerLayout
                    .Builder(R.layout.auth_method_picker_custom_layout)
                    .setGoogleButtonId(R.id.custom_google_signin_button)
                    .setEmailButtonId(R.id.custom_email_signin_clickable_text)
                    .setTosAndPrivacyPolicyId(R.id.custom_tos_pp)
                    .build();

            builder.setTheme(R.style.CustomTheme);
            builder.setAuthMethodPickerLayout(customLayout);
        }

        if (getSelectedTosUrl() != null && getSelectedPrivacyPolicyUrl() != null) {
            builder.setTosAndPrivacyPolicyUrls(
                    getSelectedTosUrl(),
                    getSelectedPrivacyPolicyUrl());
        }

        if (link != null) {
            builder.setEmailLink(link);
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null && auth.getCurrentUser().isAnonymous()) {
            builder.enableAnonymousUsersAutoUpgrade();
        }
        return builder.build();
    }

    public void silentSignIn() {
        getAuthUI().silentSignIn(this, getSelectedProviders())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startSignedInActivity(null);
                    } else {
                        showSnackbar(R.string.sign_in_failed);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && getIntent().getExtras() == null) {
            startSignedInActivity(null);
            finish();
        }
    }

    private void handleSignInResponse(int resultCode, @Nullable IdpResponse response) {
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

            if (response.getError().getErrorCode() == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
                Intent intent = new Intent(this, AnonymousUpgradeActivity.class).putExtra
                        (ExtraConstants.IDP_RESPONSE, response);
                startActivity(intent);
            }

            if (response.getError().getErrorCode() == ErrorCodes.ERROR_USER_DISABLED) {
                showSnackbar(R.string.account_disabled);
                return;
            }

            showSnackbar(R.string.unknown_error);
            Log.e(TAG, "Sign-in error: ", response.getError());
        }
    }

    private void startSignedInActivity(@Nullable IdpResponse response) {
        startActivity(SignedInActivity.createIntent(this, response));
    }

    @StyleRes
    private int getSelectedTheme() {
        if (mBinding.greenTheme.isChecked()) {
            return R.style.GreenTheme;
        }

        if (mBinding.appTheme.isChecked()) {
            return R.style.AppTheme;
        }

        return AuthUI.getDefaultTheme();
    }

    @DrawableRes
    private int getSelectedLogo() {
        if (mBinding.firebaseLogo.isChecked()) {
            return R.drawable.firebase_auth_120dp;
        } else if (mBinding.googleLogo.isChecked()) {
            return R.drawable.ic_googleg_color_144dp;
        }
        return AuthUI.NO_LOGO;
    }

    private List<IdpConfig> getSelectedProviders() {
        List<IdpConfig> selectedProviders = new ArrayList<>();

        if (mBinding.googleProvider.isChecked()) {
            selectedProviders.add(
                    new IdpConfig.GoogleBuilder().setScopes(getGoogleScopes()).build());
        }

        if (mBinding.facebookProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.FacebookBuilder()
                    .setPermissions(getFacebookPermissions())
                    .build());
        }

        if (mBinding.emailProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.EmailBuilder()
                    .setRequireName(mBinding.requireName.isChecked())
                    .setAllowNewAccounts(mBinding.allowNewEmailAccounts.isChecked())
                    .build());
        }

        if (mBinding.emailLinkProvider.isChecked()) {
            ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setAndroidPackageName("com.firebase.uidemo", true, null)
                    .setHandleCodeInApp(true)
                    .setUrl("https://google.com")
                    .build();

            selectedProviders.add(new IdpConfig.EmailBuilder()
                    .setAllowNewAccounts(mBinding.allowNewEmailAccounts.isChecked())
                    .setActionCodeSettings(actionCodeSettings)
                    .enableEmailLinkSignIn()
                    .build());
        }

        if (mBinding.phoneProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.PhoneBuilder().build());
        }

        if (mBinding.anonymousProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.AnonymousBuilder().build());
        }

        if (mBinding.twitterProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.TwitterBuilder().build());
        }

        if (mBinding.microsoftProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.MicrosoftBuilder().build());
        }

        if (mBinding.yahooProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.YahooBuilder().build());
        }

        if (mBinding.appleProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.AppleBuilder().build());
        }

        if (mBinding.githubProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.GitHubBuilder().build());
        }

        return selectedProviders;
    }

    @Nullable
    private String getSelectedTosUrl() {
        if (mBinding.googleTosPrivacy.isChecked()) {
            return GOOGLE_TOS_URL;
        }

        if (mBinding.firebaseTosPrivacy.isChecked()) {
            return FIREBASE_TOS_URL;
        }

        return null;
    }

    @Nullable
    private String getSelectedPrivacyPolicyUrl() {
        if (mBinding.googleTosPrivacy.isChecked()) {
            return GOOGLE_PRIVACY_POLICY_URL;
        }

        if (mBinding.firebaseTosPrivacy.isChecked()) {
            return FIREBASE_PRIVACY_POLICY_URL;
        }

        return null;
    }

    private void setGoogleScopesEnabled(boolean enabled) {
        mBinding.googleScopesHeader.setEnabled(enabled);
        mBinding.googleScopeDriveFile.setEnabled(enabled);
        mBinding.googleScopeYoutubeData.setEnabled(enabled);
    }

    private void setFacebookPermissionsEnabled(boolean enabled) {
        mBinding.facebookPermissionsHeader.setEnabled(enabled);
        mBinding.facebookPermissionFriends.setEnabled(enabled);
        mBinding.facebookPermissionPhotos.setEnabled(enabled);
    }

    private List<String> getGoogleScopes() {
        List<String> result = new ArrayList<>();
        if (mBinding.googleScopeYoutubeData.isChecked()) {
            result.add("https://www.googleapis.com/auth/youtube.readonly");
        }
        if (mBinding.googleScopeDriveFile.isChecked()) {
            result.add(Scopes.DRIVE_FILE);
        }
        return result;
    }

    private List<String> getFacebookPermissions() {
        List<String> result = new ArrayList<>();
        if (mBinding.facebookPermissionFriends.isChecked()) {
            result.add("user_friends");
        }
        if (mBinding.facebookPermissionPhotos.isChecked()) {
            result.add("user_photos");
        }
        return result;
    }

    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mBinding.getRoot(), errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(@NonNull FirebaseAuthUIAuthenticationResult result) {
        // Successfully signed in
        IdpResponse response = result.getIdpResponse();
        handleSignInResponse(result.getResultCode(), response);
    }
}
