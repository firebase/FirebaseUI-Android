package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.provider.AuthCredentialHelper;
import com.firebase.ui.auth.provider.FacebookProvider;
import com.firebase.ui.auth.provider.GoogleProvider;
import com.firebase.ui.auth.provider.IdpProvider;
import com.firebase.ui.auth.provider.TwitterProvider;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.util.signincontainer.ReauthDelegate;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.List;


/**
 * TODO javadoc
 */
public class ReauthActivity extends UpEnabledActivity {
    private static final int RC_CREDENTIALS_READ = 200;
    public static final String REAUTAH_REASON_ADD_EMAIL = "add_email";
    public static final String REAUTAH_REASON_CHANGE_PASSWORD = "change_password";

    private static final String TAG = "ReauthActivity";
    private static final String EXTRA_REAUTH_REASON = "reauth_reason";
    private FlowParameters mFlowParameters;
    private SaveSmartLock mSaveSmartLock;
    private IdpProvider mProvider;
    private GoogleApiClient mGoogleApiClient;

    public static Intent createIntent(
            Context context, FlowParameters flowParams, String reauthReason) {
        return BaseHelper.createBaseIntent(context, ReauthActivity.class, flowParams)
                .putExtra(EXTRA_REAUTH_REASON, reauthReason);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_reauth);
        mFlowParameters = mActivityHelper.getFlowParams();
        mSaveSmartLock = mActivityHelper.getSaveSmartLockInstance();
        FirebaseUser currentUser = mActivityHelper.getCurrentUser();
        List<String> providers = currentUser.getProviders();
        if (providers.size() == 0) {
            Log.e(TAG, "No providers for user");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // TODO maybe be smarter and find any intersection between user providers and idpconfigs
        String provider = providers.get(0);
        if (AuthUI.EMAIL_PROVIDER.equals(provider)) {
            // set up email provider
            Log.e(TAG, "EMAIL PROVIDER NOT IMPLEMENTED YET"); // TODO
        } else {
            for (AuthUI.IdpConfig idpConfig : mFlowParameters.providerInfo) {
                if (idpConfig.getProviderId().equals(provider)) {
                    mProvider = getProvider(idpConfig);
                }
            }
        }
        ReauthDelegate.delegate(this, mActivityHelper.getFlowParams());

        setupIdpButtons(provider);
        setupBodyText(getIntent().getStringExtra(EXTRA_REAUTH_REASON));
    }

    private void setupIdpButtons(String provider) {
        LinearLayout idpHolder = (LinearLayout) findViewById(R.id.idp_reauth);
        View providerButton = getProviderButton(provider, idpHolder);
        if (providerButton == null) {
            Log.e(TAG, "No button for provider " + provider);
            return;
        }
        providerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProvider.startLogin(ReauthActivity.this);
            }
        });
        mProvider.setAuthenticationCallback(new IdpProvider.IdpCallback() {
            @Override
            public void onSuccess(IdpResponse idpResponse) {
                AuthCredential credential = AuthCredentialHelper.getAuthCredential(idpResponse);
                mActivityHelper.getFirebaseAuth()
                        .signInWithCredential(credential)
                        .addOnFailureListener(
                                new TaskFailureLogger(TAG, "Firebase sign in with credential "
                                        + credential.getProvider()
                                        + " unsuccessful. Visit https://console.firebase.google.com"
                                        + " to enable it."))
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                setResult(ResultCodes.OK);
                                finish();
                            }
                        });
            }

            @Override
            public void onFailure(Bundle extra) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        idpHolder.addView(providerButton);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ReauthDelegate delegate = ReauthDelegate.getInstance(this);
        if (delegate != null) delegate.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }

    private void setupBodyText(String reauthReason) {
        TextView body = (TextView) findViewById(R.id.reauth_body);
        String reauthAction;
        if (REAUTAH_REASON_ADD_EMAIL.equals(reauthReason)) {
            reauthAction = getString(R.string.reauth_action_add_email);
        } else if (REAUTAH_REASON_CHANGE_PASSWORD.equals(reauthReason)) {
            reauthAction = getString(R.string.reauth_reason_change_password);
        } else {
            Log.e(TAG, "Invalid reauth reason: " + reauthReason);
            return;
        }
        body.setText(getResources().getString(R.string.reauth_explanation, reauthAction));
    }

    private View getProviderButton(String providerId, ViewGroup btnHolder) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                return getLayoutInflater()
                        .inflate(R.layout.idp_button_google, btnHolder, false);
            case FacebookAuthProvider.PROVIDER_ID:
                return getLayoutInflater()
                        .inflate(R.layout.idp_button_facebook, btnHolder, false);
            case TwitterAuthProvider.PROVIDER_ID:
                return getLayoutInflater()
                        .inflate(R.layout.idp_button_twitter, btnHolder, false);
            default:
                Log.e(TAG, "No button for provider " + providerId);
                return null;
        }
    }

    private IdpProvider getProvider(AuthUI.IdpConfig idpConfig) {
        switch (idpConfig.getProviderId()) {
            case AuthUI.GOOGLE_PROVIDER:
                return new GoogleProvider(this, idpConfig);
            case AuthUI.FACEBOOK_PROVIDER:
                return new FacebookProvider(
                        this, idpConfig, mActivityHelper.getFlowParams().themeId);
            case AuthUI.TWITTER_PROVIDER:
                return new TwitterProvider(this);
            case AuthUI.EMAIL_PROVIDER:
            default:
                Log.e(TAG, "Encountered unknown IDPProvider parcel with type: "
                        + idpConfig.getProviderId());
                return null;
        }
    }
}
