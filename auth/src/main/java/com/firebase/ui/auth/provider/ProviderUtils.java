package com.firebase.ui.auth.provider;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.auth.TwitterAuthProvider;

import java.util.List;

public final class ProviderUtils {
    private static final String TAG = "ProviderUtils";

    private ProviderUtils() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    public static AuthCredential getAuthCredential(IdpResponse idpResponse) {
        switch (idpResponse.getProviderType()) {
            case GoogleAuthProvider.PROVIDER_ID:
                return GoogleProvider.createAuthCredential(idpResponse);
            case FacebookAuthProvider.PROVIDER_ID:
                return FacebookProvider.createAuthCredential(idpResponse);
            case TwitterAuthProvider.PROVIDER_ID:
                return TwitterProvider.createAuthCredential(idpResponse);
            default:
                return null;
        }
    }

    public static Task<String> fetchTopProvider(FirebaseAuth auth, @NonNull String email) {
        if (TextUtils.isEmpty(email)) {
            return Tasks.forException(new NullPointerException("Email cannot be empty"));
        }

        return auth.fetchProvidersForEmail(email)
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Error fetching providers for email"))
                .continueWith(new Continuation<ProviderQueryResult, String>() {
                    @Override
                    public String then(@NonNull Task<ProviderQueryResult> task) {
                        if (!task.isSuccessful()) return null;

                        List<String> providers = task.getResult().getProviders();
                        return providers == null || providers.isEmpty()
                                ? null : providers.get(providers.size() - 1);
                    }
                });
    }
}
