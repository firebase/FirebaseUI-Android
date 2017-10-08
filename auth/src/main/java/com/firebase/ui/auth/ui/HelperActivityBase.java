package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.app.PendingIntent;
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
import android.util.Pair;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.remote.SignInHandler;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.FlowHolder;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity {
    private FlowHolder mFlowHolder;
    private SignInHandler mSignInHandler;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressDialogHolder = new ProgressDialogHolder(this);

        getSignInHandler().getSignInLiveData().observe(this, new Observer<IdpResponse>() {
            @Override
            public void onChanged(@Nullable IdpResponse response) {
                if (!response.isSuccessful()) {
                    Log.e("AuthUI", "Sign in error occurred.", response.getException());
                }
            }
        });
        getFlowHolder().getIntentStarter().observe(this, new Observer<Pair<Intent, Integer>>() {
            @Override
            public void onChanged(@Nullable Pair<Intent, Integer> request) {
                if (request == null) {
                    throw new IllegalStateException("Cannot start null request");
                }

                startActivityForResult(request.first, request.second);
            }
        });
        getFlowHolder().getPendingIntentStarter()
                .observe(this, new Observer<Pair<PendingIntent, Integer>>() {
                    @Override
                    public void onChanged(@Nullable Pair<PendingIntent, Integer> request) {
                        if (request == null) {
                            throw new IllegalStateException("Cannot start null request");
                        }

                        try {
                            startIntentSenderForResult(
                                    request.first.getIntentSender(), request.second, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("PendingIntentStarter", "Unable to start pending intent", e);
                            onActivityResult(request.second, Activity.RESULT_CANCELED, null);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getFlowHolder().onActivityResult(requestCode, resultCode, data);
    }

    public FlowHolder getFlowHolder() {
        if (mFlowHolder == null) {
            mFlowHolder = ViewModelProviders.of(this).get(FlowHolder.class);
            mFlowHolder.init(FlowParameters.fromIntent(getIntent()));
        }

        return mFlowHolder;
    }

    public SignInHandler getSignInHandler() {
        if (mSignInHandler == null) {
            mSignInHandler = ViewModelProviders.of(this).get(SignInHandler.class);
            mSignInHandler.init(getFlowHolder());
        }

        return mSignInHandler;
    }

    public ProgressDialogHolder getDialogHolder() {
        return mProgressDialogHolder;
    }

    public void finish(int resultCode, Intent intent) {
        setResult(resultCode, intent);
        finish();
    }
}
