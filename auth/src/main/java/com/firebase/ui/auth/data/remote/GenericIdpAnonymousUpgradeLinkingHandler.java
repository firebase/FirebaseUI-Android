package com.firebase.ui.auth.data.remote;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.OAuthCredential;
import com.google.firebase.auth.OAuthProvider;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GenericIdpAnonymousUpgradeLinkingHandler extends GenericIdpSignInHandler {

    public GenericIdpAnonymousUpgradeLinkingHandler(Application application) {
        super(application);
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
            handleAnonymousUpgradeLinkingFlow(activity, provider, flowParameters);
            return;
        }

        handleNormalSignInFlow(auth, activity, provider);
    }

    private void handleAnonymousUpgradeLinkingFlow(final HelperActivityBase activity,
                                                   final OAuthProvider provider,
                                                   final FlowParameters flowParameters) {
        final boolean useEmulator = activity.getAuthUI().isUseEmulator();
        AuthOperationManager.getInstance().safeGenericIdpSignIn(activity, provider, flowParameters)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Pass the credential so we can sign-in on the after the merge
                        // conflict is resolved.
                        handleSuccess(
                                useEmulator,
                                provider.getProviderId(),
                                authResult.getUser(), (OAuthCredential) authResult.getCredential(),
                                /* setPendingCredential= */true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setResult(Resource.<IdpResponse>forFailure(e));
                    }
                });

    }
}
