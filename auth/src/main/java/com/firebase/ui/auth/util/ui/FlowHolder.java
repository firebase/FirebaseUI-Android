package com.firebase.ui.auth.util.ui;

import android.app.Application;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.util.Pair;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.util.data.SingleLiveEvent;

public class FlowHolder extends ViewModelBase<FlowParameters> {
    private static MutableLiveData<ActivityResult> ACTIVITY_RESULT_LISTENER = new AutoClearActivityResultListener();

    private final MutableLiveData<Pair<Intent, Integer>> mIntentStarter = new SingleLiveEvent<>();
    private final MutableLiveData<Pair<PendingIntent, Integer>> mPendingIntentStarter = new SingleLiveEvent<>();

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
        return mIntentStarter;
    }

    public MutableLiveData<Pair<PendingIntent, Integer>> getPendingIntentStarter() {
        return mPendingIntentStarter;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ACTIVITY_RESULT_LISTENER.setValue(new ActivityResult(requestCode, resultCode, data));
    }

    public FlowParameters getParams() {
        return mFlowParams;
    }

    private static final class AutoClearActivityResultListener extends SingleLiveEvent<ActivityResult> {
        @Override
        protected void onInactive() {
            // When the all listeners are removed i.e. all sign-in activities have finished,
            // reset the listener
            ACTIVITY_RESULT_LISTENER = new AutoClearActivityResultListener();
        }
    }
}
