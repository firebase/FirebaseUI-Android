package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.arch.lifecycle.MutableLiveData;
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
import com.firebase.ui.auth.util.SignInHolder;
import com.firebase.ui.auth.util.SingleLiveEvent;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HelperActivityBase extends AppCompatActivity {
    private FlowParameters mFlowParameters;
    private SignInHolder mSignInHolder;
    private ProgressDialogHolder mProgressDialogHolder;

    private SingleLiveEvent<Pair<Intent, Integer>> mIntentStarter = new SingleLiveEvent<>();
    private SingleLiveEvent<Pair<PendingIntent, Integer>> mPendingIntentStarter = new SingleLiveEvent<>();

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
        getSignInHolder().init(Pair.create(getFlowParams(), savedInstanceState));
        mProgressDialogHolder = new ProgressDialogHolder(this);

        mIntentStarter.observe(this, new Observer<Pair<Intent, Integer>>() {
            @Override
            public void onChanged(@Nullable Pair<Intent, Integer> request) {
                if (request == null) {
                    throw new IllegalStateException("Cannot start null request");
                }

                startActivityForResult(request.first, request.second);
            }
        });
        mPendingIntentStarter.observe(this, new Observer<Pair<PendingIntent, Integer>>() {
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
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    public FlowParameters getFlowParams() {
        if (mFlowParameters == null) {
            mFlowParameters = FlowParameters.fromIntent(getIntent());
        }

        return mFlowParameters;
    }

    public SignInHolder getSignInHolder() {
        if (mSignInHolder == null) {
            mSignInHolder = ViewModelProviders.of(this).get(SignInHolder.class);
        }

        return mSignInHolder;
    }

    public ProgressDialogHolder getDialogHolder() {
        return mProgressDialogHolder;
    }

    public MutableLiveData<Pair<Intent, Integer>> getIntentStarter() {
        return mIntentStarter;
    }

    public MutableLiveData<Pair<PendingIntent, Integer>> getPendingIntentStarter() {
        return mPendingIntentStarter;
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
