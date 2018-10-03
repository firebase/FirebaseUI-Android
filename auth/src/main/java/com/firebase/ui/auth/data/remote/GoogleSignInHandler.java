package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.firebase.auth.GoogleAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GoogleSignInHandler extends ProviderSignInBase<GoogleSignInHandler.Params> {
    private static final String TAG = "GoogleSignInHandler";

    private AuthUI.IdpConfig mConfig;
    @Nullable private String mEmail;

    public GoogleSignInHandler(Application application) {
        super(application);
    }

    private static IdpResponse createIdpResponse(GoogleSignInAccount account) {
        return new IdpResponse.Builder(
                new User.Builder(GoogleAuthProvider.PROVIDER_ID, account.getEmail())
                        .setName(account.getDisplayName())
                        .setPhotoUri(account.getPhotoUrl())
                        .build())
                .setToken(account.getIdToken())
                .build();
    }

    @Override
    protected void onCreate() {
        Params params = getArguments();
        mConfig = params.config;
        mEmail = params.email;
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        start();
    }

    private void start() {
        setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                GoogleSignIn.getClient(getApplication(), getSignInOptions()).getSignInIntent(),
                RequestCodes.GOOGLE_PROVIDER)));
    }

    private GoogleSignInOptions getSignInOptions() {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(
                mConfig.getParams().<GoogleSignInOptions>getParcelable(
                        ExtraConstants.GOOGLE_SIGN_IN_OPTIONS));

        if (!TextUtils.isEmpty(mEmail)) {
            builder.setAccountName(mEmail);
        }

        return builder.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.GOOGLE_PROVIDER) { return; }

        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);
            setResult(Resource.forSuccess(createIdpResponse(account)));
        } catch (ApiException e) {
            if (e.getStatusCode() == CommonStatusCodes.INVALID_ACCOUNT) {
                // If we get INVALID_ACCOUNT, it means the pre-set account was not available on the
                // device so set the email to null and launch the sign-in picker.
                mEmail = null;
                start();
            } else if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS) {
                // Hack for https://github.com/googlesamples/google-services/issues/345
                // Google remembers the account so the picker doesn't appear twice for the user.
                start();
            } else if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
            } else {
                if (e.getStatusCode() == CommonStatusCodes.DEVELOPER_ERROR) {
                    Log.w(TAG, "Developer error: this application is misconfigured. " +
                            "Check your SHA1 and package name in the Firebase console.");
                }
                setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException(
                        ErrorCodes.PROVIDER_ERROR,
                        "Code: " + e.getStatusCode() + ", message: " + e.getMessage())));
            }
        }
    }

    public static final class Params {
        private final AuthUI.IdpConfig config;
        @Nullable private final String email;

        public Params(AuthUI.IdpConfig config) {
            this(config, null);
        }

        public Params(AuthUI.IdpConfig config, @Nullable String email) {
            this.config = config;
            this.email = email;
        }
    }
}
