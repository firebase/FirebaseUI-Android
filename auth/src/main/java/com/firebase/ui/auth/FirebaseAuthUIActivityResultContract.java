package com.firebase.ui.auth;

import android.content.Context;
import android.content.Intent;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link ActivityResultContract} describing that the caller can launch authentication with a
 * {@link com.firebase.ui.auth.AuthUI.SignInIntentBuilder} and is guaranteed to receive a
 * {@link FirebaseAuthUIAuthenticationResult} as result.
 */
public class FirebaseAuthUIActivityResultContract extends
        ActivityResultContract<AuthUI.SignInIntentBuilder, FirebaseAuthUIAuthenticationResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, AuthUI.SignInIntentBuilder input) {
        return input.build();
    }

    @Override
    public FirebaseAuthUIAuthenticationResult parseResult(int resultCode, @Nullable Intent intent) {
        return new FirebaseAuthUIAuthenticationResult(resultCode, IdpResponse.fromResultIntent(intent));
    }

}
