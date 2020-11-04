package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.credentials.CredentialSaveActivity;
import com.firebase.ui.auth.util.CredentialUtils;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class HelperActivityBase extends AppCompatActivity implements ProgressView {
    private FlowParameters mParams;

    protected static Intent createBaseIntent(
            @NonNull Context context,
            @NonNull Class<? extends Activity> target,
            @NonNull FlowParameters flowParams) {
        Intent intent = new Intent(
                checkNotNull(context, "context cannot be null"),
                checkNotNull(target, "target activity cannot be null"))
                .putExtra(ExtraConstants.FLOW_PARAMS,
                        checkNotNull(flowParams, "flowParams cannot be null"));
        intent.setExtrasClassLoader(AuthUI.class.getClassLoader());
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Forward the results of Smart Lock saving
        if (requestCode == RequestCodes.CRED_SAVE_FLOW
                || resultCode == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
            finish(resultCode, data);
        }
    }

    public FlowParameters getFlowParams() {
        if (mParams == null) {
            mParams = FlowParameters.fromIntent(getIntent());
        }
        return mParams;
    }

    public AuthUI getAuthUI() {
        return AuthUI.getInstance(getFlowParams().appName);
    }

    public FirebaseAuth getAuth() {
        return getAuthUI().getAuth();
    }

    public void finish(int resultCode, @Nullable Intent intent) {
        setResult(resultCode, intent);
        finish();
    }

    public void startSaveCredentials(
            FirebaseUser firebaseUser,
            IdpResponse response,
            @Nullable String password) {
        // Build credential
        String accountType = ProviderUtils.idpResponseToAccountType(response);
        Credential credential = CredentialUtils.buildCredential(
                firebaseUser, password, accountType);

        // Start the dedicated SmartLock Activity
        Intent intent = CredentialSaveActivity.createIntent(
                this, getFlowParams(), credential, response);
        startActivityForResult(intent, RequestCodes.CRED_SAVE_FLOW);
    }

    /**
     * Check if there is an active or soon-to-be-active network connection.
     *
     * @return true if there is no network connection, false otherwise.
     */
    protected boolean isOffline() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        return !(manager != null
                && manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnectedOrConnecting());
    }
}
