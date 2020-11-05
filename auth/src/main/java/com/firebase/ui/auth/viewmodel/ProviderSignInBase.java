package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.content.Intent;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Handles retrieving a provider's login credentials, be that a token, secret, or both.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProviderSignInBase<T> extends OperableViewModel<T, Resource<IdpResponse>> {

    protected ProviderSignInBase(Application application) {
        super(application);
    }

    /**
     * Start the login process for the IDP, e.g. show the Google sign-in activity.
     *
     * @param activity from which to start the login, DO NOT USE OUTSIDE OF THIS METHOD!!!
     */
    public abstract void startSignIn(@NonNull HelperActivityBase activity);


    /**
     * Start the login process for the IDP using the web based Generic IDP flow if applicable,
     * e.g. sign-in through a custom-chrome tab, otherwise falls back to the default method.
     *
     * @param auth       the Firebase auth instance
     * @param activity   from which to start the login, DO NOT USE OUTSIDE OF THIS METHOD!!!
     * @param providerId the provider to sign-in with (e.g. "microsoft.com")
     */
    public abstract void startSignIn(@NonNull FirebaseAuth auth,
                                     @NonNull HelperActivityBase activity,
                                     @NonNull String providerId);

    public abstract void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

    /**
     * Just a convenience method that makes certain chaining logic easier.
     */
    public ProviderSignInBase<T> initWith(T args) {
        super.init(args);
        return this;
    }
}
