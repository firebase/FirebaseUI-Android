package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.idp.ProviderHandler;
import com.firebase.ui.auth.viewmodel.idp.ProviderParamsBase;
import com.firebase.ui.auth.viewmodel.idp.ProvidersHandlerBase;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.firebase.auth.GoogleAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GoogleSignInHandler extends ProviderHandler<GoogleSignInHandler.Params> {
    private static final String TAG = "GoogleSignInHandler";

    private MutableLiveData<Resource<Intent>> mRequest = new MutableLiveData<>();

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

    public void start() {
        mRequest.setValue(Resource.forSuccess(
                GoogleSignIn.getClient(getApplication(), getSignInOptions()).getSignInIntent()));
    }

    public LiveData<Resource<Intent>> getRequest() {
        return mRequest;
    }

    private GoogleSignInOptions getSignInOptions() {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(
                mConfig.getParams().<GoogleSignInOptions>getParcelable(
                        ExtraConstants.EXTRA_GOOGLE_SIGN_IN_OPTIONS));

        if (!TextUtils.isEmpty(mEmail)) {
            builder.setAccountName(mEmail);
        }

        return builder.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException.class);
            setResult(createIdpResponse(account));
        } catch (ApiException e) {
            if (e.getStatusCode() == CommonStatusCodes.INVALID_ACCOUNT) {
                // If we get INVALID_ACCOUNT, it means the pre-set account was not available on the
                // device so set the email to null and launch the sign-in picker.
                mEmail = null;
                start();
            } else {
                if (e.getStatusCode() == CommonStatusCodes.DEVELOPER_ERROR) {
                    Log.w(TAG, "Developer error: this application is misconfigured. " +
                            "Check your SHA1 and package name in the Firebase console.");
                }
                setResult(IdpResponse.fromError(new FirebaseUiException(
                        ErrorCodes.PROVIDER_ERROR,
                        "Code: " + e.getStatusCode() + ", message: " + e.getMessage())));
            }
        }
    }

    public static final class Params extends ProviderParamsBase {
        private final AuthUI.IdpConfig config;
        @Nullable private final String email;

        public Params(ProvidersHandlerBase handler, AuthUI.IdpConfig config, @Nullable String email) {
            super(handler);
            this.config = config;
            this.email = email;
        }
    }
}
