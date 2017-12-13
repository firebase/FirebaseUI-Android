package com.firebase.ui.auth.util.ui;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.ActivityResult;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.IntentRequest;
import com.firebase.ui.auth.data.model.PendingIntentRequest;
import com.firebase.ui.auth.util.data.AutoClearSingleLiveEvent;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FlowHolder extends ViewModelBase<FlowParameters> {
    private static MutableLiveData<ActivityResult> ACTIVITY_RESULT_LISTENER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    ACTIVITY_RESULT_LISTENER = new AutoClearSingleLiveEvent<>(this);
                }
            });
    private static MutableLiveData<IntentRequest> INTENT_STARTER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    INTENT_STARTER = new AutoClearSingleLiveEvent<>(this);
                }
            });
    private static MutableLiveData<PendingIntentRequest> PENDING_INTENT_STARTER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    PENDING_INTENT_STARTER = new AutoClearSingleLiveEvent<>(this);
                }
            });

    private final MutableLiveData<Boolean> mProgressListener = new MutableLiveData<>();

    private FlowParameters mFlowParams;

    public FlowHolder(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowParameters args) {
        mFlowParams = args;
    }

    public LiveData<ActivityResult> getActivityResultListener() {
        return ACTIVITY_RESULT_LISTENER;
    }

    public MutableLiveData<Boolean> getProgressListener() {
        return mProgressListener;
    }

    public MutableLiveData<IntentRequest> getIntentStarter() {
        return INTENT_STARTER;
    }

    public MutableLiveData<PendingIntentRequest> getPendingIntentStarter() {
        return PENDING_INTENT_STARTER;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ACTIVITY_RESULT_LISTENER.setValue(new ActivityResult(requestCode, resultCode, data));
    }

    public FlowParameters getParams() {
        return mFlowParams;
    }
}
