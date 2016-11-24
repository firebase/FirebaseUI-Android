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
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.AuthUI.IdpConfig;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.ResultCodes;
import com.firebase.uidemo.R;
import com.google.android.gms.common.Scopes;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AuthUiActivity extends AppCompatActivity {
    private static final String UNCHANGED_CONFIG_VALUE = "CHANGE-ME";
    private static final String GOOGLE_TOS_URL = "https://www.google.com/policies/terms/";
    private static final String FIREBASE_TOS_URL = "https://www.firebase.com/terms/terms-of-service.html";
    private static final int RC_SIGN_IN = 100;

    @BindView(R.id.default_theme)
    RadioButton mUseDefaultTheme;

    @BindView(R.id.green_theme)
    RadioButton mUseGreenTheme;

    @BindView(R.id.purple_theme)
    RadioButton mUsePurpleTheme;

    @BindView(R.id.dark_theme)
    RadioButton mUseDarkTheme;

    @BindView(R.id.email_provider)
    CheckBox mUseEmailProvider;

    @BindView(R.id.google_provider)
    CheckBox mUseGoogleProvider;

    @BindView(R.id.facebook_provider)
    CheckBox mUseFacebookProvider;

    @BindView(R.id.twitter_provider)
    CheckBox mUseTwitterProvider;

    @BindView(R.id.google_tos)
    RadioButton mUseGoogleTos;

    @BindView(R.id.firebase_tos)
    RadioButton mUseFirebaseTos;

    @BindView(R.id.sign_in)
    Button mSignIn;

    @BindView(android.R.id.content)
    View mRootView;

    @BindView(R.id.firebase_logo)
    RadioButton mFirebaseLogo;

    @BindView(R.id.google_logo)
    RadioButton mGoogleLogo;

    @BindView(R.id.no_logo)
    RadioButton mNoLogo;

    @BindView(R.id.smartlock_enabled)
    CheckBox mEnableSmartLock;

    @BindView(R.id.facebook_scopes_label)
    TextView mFacebookScopesLabel;

    @BindView(R.id.facebook_scope_friends)
    CheckBox mFacebookScopeFriends;

    @BindView(R.id.facebook_scope_photos)
    CheckBox mFacebookScopePhotos;

    @BindView(R.id.google_scopes_label)
    TextView mGoogleScopesLabel;

    @BindView(R.id.google_scope_drive_file)
    CheckBox mGoogleScopeDriveFile;

    @BindView(R.id.google_scope_games)
    CheckBox mGoogleScopeGames;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(SignedInActivity.createIntent(this, null));
            finish();
        }

        setContentView(R.layout.auth_ui_layout);
        ButterKnife.bind(this);

        if (!isGoogleConfigured()) {
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

        if (!isFacebookConfigured()) {
            mUseFacebookProvider.setChecked(false);
            mUseFacebookProvider.setEnabled(false);
            mUseFacebookProvider.setText(R.string.facebook_label_missing_config);
            setFacebookScopesEnabled(false);
        } else {
            setFacebookScopesEnabled(mUseFacebookProvider.isChecked());
            mUseFacebookProvider.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    setFacebookScopesEnabled(checked);
                }
            });
        }

        if (!isTwitterConfigured()) {
            mUseTwitterProvider.setChecked(false);
            mUseTwitterProvider.setEnabled(false);
            mUseTwitterProvider.setText(R.string.twitter_label_missing_config);
        }

        if (!isGoogleConfigured() || !isFacebookConfigured() || !isTwitterConfigured()) {
            showSnackbar(R.string.configuration_required);
        }
    }

    @OnClick(R.id.sign_in)
    public void signIn(View view) {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setTheme(getSelectedTheme())
                        .setLogo(getSelectedLogo())
                        .setProviders(getSelectedProviders())
                        .setTosUrl(getSelectedTosUrl())
                        .setIsSmartLockEnabled(mEnableSmartLock.isChecked())
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
            return;
        }

        showSnackbar(R.string.unknown_response);
    }

    @MainThread
    private void handleSignInResponse(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            startActivity(SignedInActivity.createIntent(this, IdpResponse.fromResultIntent(data)));
            finish();
            return;
        }

        if (resultCode == RESULT_CANCELED) {
            showSnackbar(R.string.sign_in_cancelled);
            return;
        }

        if (resultCode == ResultCodes.RESULT_NO_NETWORK) {
            showSnackbar(R.string.no_internet_connection);
            return;
        }

        showSnackbar(R.string.unknown_sign_in_response);
    }

    @MainThread
    private void setGoogleScopesEnabled(boolean enabled) {
        mGoogleScopesLabel.setEnabled(enabled);
        mGoogleScopeDriveFile.setEnabled(enabled);
        mGoogleScopeGames.setEnabled(enabled);
    }

    @MainThread
    private void setFacebookScopesEnabled(boolean enabled) {
        mFacebookScopesLabel.setEnabled(enabled);
        mFacebookScopeFriends.setEnabled(enabled);
        mFacebookScopePhotos.setEnabled(enabled);
    }

    @MainThread
    @StyleRes
    private int getSelectedTheme() {
        if (mUseDefaultTheme.isChecked()) {
            return AuthUI.getDefaultTheme();
        }

        if (mUsePurpleTheme.isChecked()) {
            return R.style.PurpleTheme;
        }

        if (mUseDarkTheme.isChecked()) {
            return R.style.DarkTheme;
        }

        return R.style.GreenTheme;
    }

    @MainThread
    @DrawableRes
    private int getSelectedLogo() {
        if (mFirebaseLogo.isChecked()) {
            return R.drawable.firebase_auth_120dp;
        } else if (mGoogleLogo.isChecked()) {
            return R.drawable.logo_googleg_color_144dp;
        }
        return AuthUI.NO_LOGO;
    }

    @MainThread
    private List<IdpConfig> getSelectedProviders() {
        List<IdpConfig> selectedProviders = new ArrayList<>();

        if (mUseEmailProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());
        }

        if (mUseFacebookProvider.isChecked()) {
            selectedProviders.add(
                    new IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER)
                            .setPermissions(getFacebookPermissions())
                            .build());
        }

        if (mUseGoogleProvider.isChecked()) {
            selectedProviders.add(
                    new IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER)
                            .setPermissions(getGooglePermissions())
                            .build());
        }

        if (mUseTwitterProvider.isChecked()) {
            selectedProviders.add(new IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build());
        }

        return selectedProviders;
    }

    @MainThread
    private String getSelectedTosUrl() {
        if (mUseGoogleTos.isChecked()) {
            return GOOGLE_TOS_URL;
        }

        return FIREBASE_TOS_URL;
    }

    @MainThread
    private boolean isGoogleConfigured() {
        return !UNCHANGED_CONFIG_VALUE.equals(
                getResources().getString(R.string.default_web_client_id));
    }

    @MainThread
    private boolean isFacebookConfigured() {
        return !UNCHANGED_CONFIG_VALUE.equals(
                getResources().getString(R.string.facebook_application_id));
    }

    @MainThread
    private boolean isTwitterConfigured() {
        List<String> twitterConfigs = Arrays.asList(
                getResources().getString(R.string.twitter_consumer_key),
                getResources().getString(R.string.twitter_consumer_secret)
        );

        return !twitterConfigs.contains(UNCHANGED_CONFIG_VALUE);
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @MainThread
    private List<String> getFacebookPermissions() {
        List<String> result = new ArrayList<>();
        if (mFacebookScopeFriends.isChecked()) {
            result.add("user_friends");
        }
        if (mFacebookScopePhotos.isChecked()) {
            result.add("user_photos");
        }
        return result;
    }

    @MainThread
    private List<String> getGooglePermissions() {
        List<String> result = new ArrayList<>();
        if (mGoogleScopeGames.isChecked()) {
            result.add(Scopes.GAMES);
        }
        if (mGoogleScopeDriveFile.isChecked()) {
            result.add(Scopes.DRIVE_FILE);
        }
        return result;
    }

    public static Intent createIntent(Context context) {
        Intent in = new Intent();
        in.setClass(context, AuthUiActivity.class);
        return in;
    }
}
