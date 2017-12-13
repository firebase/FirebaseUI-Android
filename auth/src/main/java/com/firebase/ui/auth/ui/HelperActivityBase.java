package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.IntentRequest;
import com.firebase.ui.auth.data.model.PendingIntentRequest;
import com.firebase.ui.auth.util.AuthHelper;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.firebase.ui.auth.util.ui.FlowHolder;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity {

    private FlowHolder mFlowHolder;

    private FlowParameters mFlowParameters;
    private AuthHelper mAuthHelper;
    private ProgressDialogHolder mProgressDialogHolder;

    public static Intent createBaseIntent(
            @NonNull Context context,
            @NonNull Class<? extends Activity> target,
            @NonNull FlowParameters flowParams) {
        return new Intent(
                checkNotNull(context, "context cannot be null"),
                checkNotNull(target, "target activity cannot be null"))
                .putExtra(ExtraConstants.EXTRA_FLOW_PARAMS,
                        checkNotNull(flowParams, "flowParams cannot be null"));
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mAuthHelper = new AuthHelper(getFlowParams());
        mProgressDialogHolder = new ProgressDialogHolder(this);

        getFlowHolder().getIntentStarter().observe(this, new Observer<IntentRequest>() {
            @Override
            public void onChanged(@Nullable IntentRequest request) {
                if (request == null) {
                    throw new IllegalStateException("Cannot start null request");
                }

                startActivityForResult(request.getIntent(), request.getRequestCode());
            }
        });
        getFlowHolder().getPendingIntentStarter()
                .observe(this, new Observer<PendingIntentRequest>() {
                    @Override
                    public void onChanged(@Nullable PendingIntentRequest request) {
                        if (request == null) {
                            throw new IllegalStateException("Cannot start null request");
                        }

                        try {
                            startIntentSenderForResult(
                                    request.getIntent().getIntentSender(), request.getRequestCode(),
                                    null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("PendingIntentStarter", "Unable to start pending intent", e);
                            onActivityResult(
                                    request.getRequestCode(), Activity.RESULT_CANCELED, null);
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getFlowHolder().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    public FlowHolder getFlowHolder() {
        if (mFlowHolder == null) {
            mFlowHolder = ViewModelProviders.of(this).get(FlowHolder.class);
            mFlowHolder.init(FlowParameters.fromIntent(getIntent()));
        }

        return mFlowHolder;
    }

    public FlowParameters getFlowParams() {
        if (mFlowParameters == null) {
            mFlowParameters = FlowParameters.fromIntent(getIntent());
        }

        return mFlowParameters;
    }

    public AuthHelper getAuthHelper() {
        return mAuthHelper;
    }

    public ProgressDialogHolder getDialogHolder() {
        return mProgressDialogHolder;
    }

    public void finish(int resultCode, Intent intent) {
        setResult(resultCode, intent);
        finish();
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            FirebaseUser firebaseUser,
            IdpResponse response) {
        saveCredentialsOrFinish(saveSmartLock, firebaseUser, null, response);
    }

    public void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            FirebaseUser firebaseUser,
            @Nullable String password,
            IdpResponse response) {

        if (saveSmartLock == null) {
            finish(Activity.RESULT_OK, response.toIntent());
        } else {
            saveSmartLock.saveCredentialsOrFinish(firebaseUser, password, response);
        }
    }

}
