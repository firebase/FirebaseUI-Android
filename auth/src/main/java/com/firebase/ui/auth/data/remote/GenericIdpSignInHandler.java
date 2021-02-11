package com.firebase.ui.auth.data.remote;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.FirebaseUiUserCollisionException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.firebase.ui.auth.AuthUI;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.FirebaseAuthError;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthCredential;
import com.google.firebase.auth.OAuthProvider;

import java.util.HashMap;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GenericIdpSignInHandler extends ProviderSignInBase<AuthUI.IdpConfig> {

    public GenericIdpSignInHandler(Application application) {
        super(application);
    }

    @NonNull
    public static AuthUI.IdpConfig getGenericGoogleConfig() {
        return new AuthUI.IdpConfig.GenericOAuthProviderBuilder(
                GoogleAuthProvider.PROVIDER_ID,
                "Google",
                R.layout.fui_idp_button_google
        ).build();
    }

    @NonNull
    public static AuthUI.IdpConfig getGenericFacebookConfig() {
        return new AuthUI.IdpConfig.GenericOAuthProviderBuilder(
                FacebookAuthProvider.PROVIDER_ID,
                "Facebook",
                R.layout.fui_idp_button_facebook
        ).build();
    }

    @Override
    public final void startSignIn(@NonNull HelperActivityBase activity) {
        setResult(Resource.<IdpResponse>forLoading());
        startSignIn(activity.getAuth(), activity, getArguments().getProviderId());
    }

    @Override
    public void startSignIn(@NonNull FirebaseAuth auth,
                            @NonNull HelperActivityBase activity,
                            @NonNull String providerId) {
        setResult(Resource.<IdpResponse>forLoading());

        FlowParameters flowParameters = activity.getFlowParams();
        OAuthProvider provider = buildOAuthProvider(providerId, auth);
        if (flowParameters != null
                && AuthOperationManager.getInstance().canUpgradeAnonymous(auth, flowParameters)) {
            handleAnonymousUpgradeFlow(auth, activity, provider, flowParameters);
            return;
        }

        handleNormalSignInFlow(auth, activity, provider);
    }

    protected void handleNormalSignInFlow(final FirebaseAuth auth,
                                          final HelperActivityBase activity,
                                          final OAuthProvider provider) {
        final boolean useEmulator = activity.getAuthUI().isUseEmulator();
        auth.startActivityForSignInWithProvider(activity, provider)
                .addOnSuccessListener(
                        new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(@NonNull AuthResult authResult) {
                                handleSuccess(
                                        useEmulator,
                                        provider.getProviderId(),
                                        authResult.getUser(),
                                        (OAuthCredential) authResult.getCredential(),
                                        authResult.getAdditionalUserInfo().isNewUser());
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (e instanceof FirebaseAuthException) {
                                    FirebaseAuthError error =
                                            FirebaseAuthError.fromException((FirebaseAuthException) e);

                                    if (e instanceof FirebaseAuthUserCollisionException) {
                                        FirebaseAuthUserCollisionException collisionException =
                                                (FirebaseAuthUserCollisionException) e;

                                        setResult(Resource.<IdpResponse>forFailure(
                                                new FirebaseUiUserCollisionException(
                                                        ErrorCodes.ERROR_GENERIC_IDP_RECOVERABLE_ERROR,
                                                        "Recoverable error.",
                                                        provider.getProviderId(),
                                                        collisionException.getEmail(),
                                                        collisionException.getUpdatedCredential())));
                                    } else if (error == FirebaseAuthError.ERROR_WEB_CONTEXT_CANCELED) {
                                        setResult(Resource.<IdpResponse>forFailure(
                                                new UserCancellationException()));
                                    } else {
                                        setResult(Resource.<IdpResponse>forFailure(e));
                                    }
                                } else {
                                    setResult(Resource.<IdpResponse>forFailure(e));
                                }
                            }
                        });

    }


    private void handleAnonymousUpgradeFlow(final FirebaseAuth auth,
                                            final HelperActivityBase activity,
                                            final OAuthProvider provider,
                                            final FlowParameters flowParameters) {
        final boolean useEmulator = activity.getAuthUI().isUseEmulator();
        auth.getCurrentUser()
                .startActivityForLinkWithProvider(activity, provider)
                .addOnSuccessListener(
                        new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(@NonNull AuthResult authResult) {
                                handleSuccess(
                                        useEmulator,
                                        provider.getProviderId(),
                                        authResult.getUser(),
                                        (OAuthCredential) authResult.getCredential(),
                                        authResult.getAdditionalUserInfo().isNewUser());
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (!(e instanceof FirebaseAuthUserCollisionException)) {
                                    setResult(Resource.<IdpResponse>forFailure(e));
                                    return;
                                }

                                FirebaseAuthUserCollisionException collisionException =
                                        (FirebaseAuthUserCollisionException) e;
                                final AuthCredential credential =
                                        collisionException.getUpdatedCredential();
                                final String email =
                                        collisionException.getEmail();

                                // Case 1: Anonymous user trying to link with an existing user
                                // Case 2: Anonymous user trying to link with a provider keyed
                                // by an email that already belongs to an existing account
                                // (linking flow)
                                ProviderUtils.fetchSortedProviders(auth, flowParameters, email)
                                        .addOnSuccessListener(new OnSuccessListener<List<String>>() {
                                            @Override
                                            public void onSuccess(List<String> providers) {
                                                if (providers.isEmpty()) {
                                                    String errorMessage =
                                                            "Unable to complete the linkingflow -" +
                                                                    " the user is using " +
                                                                    "unsupported providers.";
                                                    setResult(Resource.<IdpResponse>forFailure(
                                                            new FirebaseUiException(
                                                                    ErrorCodes.DEVELOPER_ERROR,
                                                                    errorMessage)));
                                                    return;
                                                }

                                                if (providers.contains(provider.getProviderId())) {
                                                    // Case 1
                                                    handleMergeFailure(credential);
                                                } else {
                                                    // Case 2 - linking flow to be handled by
                                                    // SocialProviderResponseHandler
                                                    setResult(Resource.<IdpResponse>forFailure(
                                                            new FirebaseUiUserCollisionException(
                                                                    ErrorCodes.ERROR_GENERIC_IDP_RECOVERABLE_ERROR,
                                                                    "Recoverable error.",
                                                                    provider.getProviderId(),
                                                                    email,
                                                                    credential)));
                                                }
                                            }
                                        });
                            }
                        });
    }

    public OAuthProvider buildOAuthProvider(String providerId, FirebaseAuth auth) {
        OAuthProvider.Builder providerBuilder =
                OAuthProvider.newBuilder(providerId, auth);

        List<String> scopes =
                getArguments().getParams().getStringArrayList(ExtraConstants.GENERIC_OAUTH_SCOPES);

        // This unchecked cast is safe, this extra is put in as a serializable
        // in AuthUI.setCustomParameters
        HashMap<String, String> customParams =
                (HashMap<String, String>) getArguments().getParams()
                        .getSerializable(ExtraConstants.GENERIC_OAUTH_CUSTOM_PARAMETERS);

        if (scopes != null) {
            providerBuilder.setScopes(scopes);
        }
        if (customParams != null) {
            providerBuilder.addCustomParameters(customParams);
        }

        return providerBuilder.build();
    }

    protected void handleSuccess(boolean isUseEmulator,
                                 @NonNull String providerId,
                                 @NonNull FirebaseUser user,
                                 @NonNull OAuthCredential credential,
                                 boolean isNewUser,
                                 boolean setPendingCredential) {

        String accessToken = credential.getAccessToken();
        // noinspection ConstantConditions
        if (accessToken == null && isUseEmulator) {
            accessToken = "fake_access_token";
        }

        String secret = credential.getSecret();
        if (secret == null && isUseEmulator) {
            secret = "fake_secret";
        }

        IdpResponse.Builder response = new IdpResponse.Builder(
                new User.Builder(
                        providerId, user.getEmail())
                        .setName(user.getDisplayName())
                        .setPhotoUri(user.getPhotoUrl())
                        .build())
                .setToken(accessToken)
                .setSecret(secret);

        if (setPendingCredential) {
            response.setPendingCredential(credential);
        }
        response.setNewUser(isNewUser);

        setResult(Resource.<IdpResponse>forSuccess(response.build()));
    }

    protected void handleSuccess(boolean isUseEmulator,
                                 @NonNull String providerId,
                                 @NonNull FirebaseUser user,
                                 @NonNull OAuthCredential credential,
                                 boolean isNewUser) {
        handleSuccess(isUseEmulator, providerId, user, credential, isNewUser, /* setPendingCredential= */true);
    }


    protected void handleMergeFailure(@NonNull AuthCredential credential) {
        IdpResponse failureResponse = new IdpResponse.Builder()
                .setPendingCredential(credential).build();
        setResult(Resource.<IdpResponse>forFailure(new FirebaseAuthAnonymousUpgradeException(
                ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT,
                failureResponse)));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.GENERIC_IDP_SIGN_IN_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                setResult(Resource.<IdpResponse>forFailure(new UserCancellationException()));
            } else {
                setResult(Resource.forSuccess(response));
            }
        }
    }

    @VisibleForTesting
    public void initializeForTesting(AuthUI.IdpConfig idpConfig) {
        setArguments(idpConfig);
    }
}
