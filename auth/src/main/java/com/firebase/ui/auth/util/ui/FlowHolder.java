package com.firebase.ui.auth.util.ui;

import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.util.Pair;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.util.data.AutoClearSingleLiveEvent;

public class FlowHolder extends ViewModelBase<FlowParameters> {
    private static MutableLiveData<ActivityResult> ACTIVITY_RESULT_LISTENER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    ACTIVITY_RESULT_LISTENER = new AutoClearSingleLiveEvent<>(this);
                }
            });
    private static MutableLiveData<Pair<Intent, Integer>> INTENT_STARTER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    INTENT_STARTER = new AutoClearSingleLiveEvent<>(this);
                }
            });
    private static MutableLiveData<Pair<PendingIntent, Integer>> PENDING_INTENT_STARTER =
            new AutoClearSingleLiveEvent<>(new Runnable() {
                @Override
                public void run() {
                    PENDING_INTENT_STARTER = new AutoClearSingleLiveEvent<>(this);
                }
            });

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

    public MutableLiveData<Pair<Intent, Integer>> getIntentStarter() {
        return INTENT_STARTER;
    }

    public MutableLiveData<Pair<PendingIntent, Integer>> getPendingIntentStarter() {
        return PENDING_INTENT_STARTER;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ACTIVITY_RESULT_LISTENER.setValue(new ActivityResult(requestCode, resultCode, data));
    }

    public FlowParameters getParams() {
        return mFlowParams;
    }
}
