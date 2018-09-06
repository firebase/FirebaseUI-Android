package com.firebase.uidemo.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.uidemo.R;
import com.firebase.uidemo.util.ConfigurationUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AnonymousUpgradeActivity extends AppCompatActivity {

    private static final String TAG = "AccountLink";

    private static final int RC_SIGN_IN = 123;

    @BindView(R.id.status_text)
    TextView mStatus;

    @BindView(R.id.anon_sign_in)
    Button mAnonSignInButton;

    @BindView(R.id.begin_flow)
    Button mLaunchUIButton;

    @BindView(R.id.resolve_merge)
    Button mResolveMergeButton;

    @BindView(R.id.sign_out)
    Button mSignOutButton;

    private AuthCredential mPendingCredential;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_upgrade);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.anon_sign_in)
    public void signInAnonymously() {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        updateUI();

                        if (task.isSuccessful()) {
                            setStatus("Signed in anonymously as user "
                                    + getUserIdentifier(task.getResult().getUser()));
                        } else {
                            setStatus("Anonymous sign in failed.");
                        }
                    }
                });
    }

    @OnClick(R.id.begin_flow)
    public void startAuthUI() {
        List<AuthUI.IdpConfig> providers = ConfigurationUtils.getConfiguredProviders(this);
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
            .setLogo(R.drawable.firebase_auth_120dp)
            .setAvailableProviders(providers)
            .enableAnonymousUsersAutoUpgrade()
            .build();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @OnClick(R.id.resolve_merge)
    public void resolveMerge() {
        if (mPendingCredential == null) {
            Toast.makeText(this, "Nothing to resolve.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Show how to do good data moving

        FirebaseAuth.getInstance().signInWithCredential(mPendingCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        mPendingCredential = null;
                        updateUI();

                        if (task.isSuccessful()) {
                            setStatus("Signed in as " + getUserIdentifier(task.getResult().getUser()));
                        } else {
                            Log.w(TAG, "Merge failed", task.getException());
                            setStatus("Failed to resolve merge conflict, see logs.");
                        }
                    }
                });
    }

    @OnClick(R.id.sign_out)
    public void signOut() {
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        setStatus(null);
                        updateUI();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                // User pressed back button
                return;
            }
            if (resultCode == RESULT_OK) {
                setStatus("Signed in as " + getUserIdentifier(FirebaseAuth.getInstance().getCurrentUser()));
            } else if (response.getError().getErrorCode() == ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT) {
                setStatus("Merge conflict: user already exists.");
                mResolveMergeButton.setEnabled(true);
                mPendingCredential = response.getCredentialForLinking();
            } else {
                Toast.makeText(this, "Auth error, see logs", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Error: " + response.getError().getMessage(), response.getError());
            }

            updateUI();
        }
    }

    private void updateUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            // Not signed in
            mAnonSignInButton.setEnabled(true);
            mLaunchUIButton.setEnabled(false);
            mResolveMergeButton.setEnabled(false);
            mSignOutButton.setEnabled(false);
        } else if (mPendingCredential == null && currentUser.isAnonymous()) {
            // Anonymous user, waiting for linking
            mAnonSignInButton.setEnabled(false);
            mLaunchUIButton.setEnabled(true);
            mResolveMergeButton.setEnabled(false);
            mSignOutButton.setEnabled(true);
        } else if (mPendingCredential == null && !currentUser.isAnonymous()) {
            // Fully signed in
            mAnonSignInButton.setEnabled(false);
            mLaunchUIButton.setEnabled(false);
            mResolveMergeButton.setEnabled(false);
            mSignOutButton.setEnabled(true);
        } else if (mPendingCredential != null) {
            // Signed in anonymous, awaiting merge conflict
            mAnonSignInButton.setEnabled(false);
            mLaunchUIButton.setEnabled(false);
            mResolveMergeButton.setEnabled(true);
            mSignOutButton.setEnabled(true);
        }
    }

    private void setStatus(String message) {
        mStatus.setText(message);
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
}
