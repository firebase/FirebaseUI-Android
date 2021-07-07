package com.firebase.uidemo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.firebase.uidemo.R;
import com.firebase.uidemo.databinding.ActivityAnonymousUpgradeBinding;
import com.firebase.uidemo.util.ConfigurationUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AnonymousUpgradeActivity extends AppCompatActivity
        implements ActivityResultCallback<FirebaseAuthUIAuthenticationResult> {

    private static final String TAG = "AccountLink";

    private ActivityAnonymousUpgradeBinding mBinding;

    private AuthCredential mPendingCredential;

    private final ActivityResultLauncher<Intent> signIn =
            registerForActivityResult(new FirebaseAuthUIActivityResultContract(), this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAnonymousUpgradeBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        updateUI();

        // Got here from AuthUIActivity, and we need to deal with a merge conflict
        // Occurs after catching an email link
        IdpResponse response = IdpResponse.fromResultIntent(getIntent());
        if (response != null) {
            handleSignInResult(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT, response);
        }

        mBinding.anonSignIn.setOnClickListener(view -> signInAnonymously());
        mBinding.beginFlow.setOnClickListener(view -> startAuthUI());
        mBinding.resolveMerge.setOnClickListener(view -> resolveMerge());
        mBinding.signOut.setOnClickListener(view -> signOut());
    }

    public void signInAnonymously() {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    updateUI();

                    if (task.isSuccessful()) {
                        setStatus("Signed in anonymously as user "
                                + getUserIdentifier(task.getResult().getUser()));
                    } else {
                        setStatus("Anonymous sign in failed.");
                    }
                });
    }

    public void startAuthUI() {
        List<AuthUI.IdpConfig> providers = ConfigurationUtils.getConfiguredProviders(this);
        Intent signInIntent = AuthUI.getInstance().createSignInIntentBuilder()
                .setLogo(R.drawable.firebase_auth_120dp)
                .setAvailableProviders(providers)
                .enableAnonymousUsersAutoUpgrade()
                .build();
        signIn.launch(signInIntent);
    }

    public void resolveMerge() {
        if (mPendingCredential == null) {
            Toast.makeText(this, "Nothing to resolve.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Show how to do good data moving

        FirebaseAuth.getInstance().signInWithCredential(mPendingCredential)
                .addOnCompleteListener(this, task -> {
                    mPendingCredential = null;
                    updateUI();

                    if (task.isSuccessful()) {
                        setStatus("Signed in as " + getUserIdentifier(task.getResult()
                                .getUser()));
                    } else {
                        Log.w(TAG, "Merge failed", task.getException());
                        setStatus("Failed to resolve merge conflict, see logs.");
                    }
                });
    }

    public void signOut() {
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(task -> {
                    setStatus(null);
                    updateUI();
                });
    }

    private void handleSignInResult(int resultCode, @Nullable IdpResponse response) {
        if (response == null) {
            // User pressed back button
            return;
        }
        if (resultCode == RESULT_OK) {
            setStatus("Signed in as " + getUserIdentifier(FirebaseAuth.getInstance()
                    .getCurrentUser()));
        } else if (response.getError().getErrorCode() == ErrorCodes
                .ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
            setStatus("Merge conflict: user already exists.");
            mBinding.resolveMerge.setEnabled(true);
            mPendingCredential = response.getCredentialForLinking();
        } else {
            Toast.makeText(this, "Auth error, see logs", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Error: " + response.getError().getMessage(), response.getError());
        }

        updateUI();
    }

    private void updateUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            // Not signed in
            mBinding.anonSignIn.setEnabled(true);
            mBinding.beginFlow.setEnabled(false);
            mBinding.resolveMerge.setEnabled(false);
            mBinding.signOut.setEnabled(false);
        } else if (mPendingCredential == null && currentUser.isAnonymous()) {
            // Anonymous user, waiting for linking
            mBinding.anonSignIn.setEnabled(false);
            mBinding.beginFlow.setEnabled(true);
            mBinding.resolveMerge.setEnabled(false);
            mBinding.signOut.setEnabled(true);
        } else if (mPendingCredential == null && !currentUser.isAnonymous()) {
            // Fully signed in
            mBinding.anonSignIn.setEnabled(false);
            mBinding.beginFlow.setEnabled(false);
            mBinding.resolveMerge.setEnabled(false);
            mBinding.signOut.setEnabled(true);
        } else if (mPendingCredential != null) {
            // Signed in anonymous, awaiting merge conflict
            mBinding.anonSignIn.setEnabled(false);
            mBinding.beginFlow.setEnabled(false);
            mBinding.resolveMerge.setEnabled(true);
            mBinding.signOut.setEnabled(true);
        }
    }

    private void setStatus(String message) {
        mBinding.statusText.setText(message);
    }

    private String getUserIdentifier(FirebaseUser user) {
        if (user.isAnonymous()) {
            return user.getUid();
        } else if (!TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        } else if (!TextUtils.isEmpty(user.getPhoneNumber())) {
            return user.getPhoneNumber();
        } else {
            return "unknown";
        }
    }

    @Override
    public void onActivityResult(@NonNull FirebaseAuthUIAuthenticationResult result) {
        handleSignInResult(result.getResultCode(), result.getIdpResponse());
    }
}
