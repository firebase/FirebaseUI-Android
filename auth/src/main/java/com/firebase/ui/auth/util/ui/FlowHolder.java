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
    private final MutableLiveData<ActivityResult> mOnActivityResult = new SingleLiveEvent<>();

    private final SingleLiveEvent<Pair<Intent, Integer>> mIntentStarter = new SingleLiveEvent<>();
    private final SingleLiveEvent<Pair<PendingIntent, Integer>> mPendingIntentStarter = new SingleLiveEvent<>();

    private FlowParameters mFlowParams;

    public FlowHolder(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowParameters args) {
        mFlowParams = args;
    }

    public LiveData<ActivityResult> getOnActivityResult() {
        return mOnActivityResult;
    }

    public MutableLiveData<Pair<Intent, Integer>> getIntentStarter() {
        return mIntentStarter;
    }

    public MutableLiveData<Pair<PendingIntent, Integer>> getPendingIntentStarter() {
        return mPendingIntentStarter;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mOnActivityResult.setValue(new ActivityResult(requestCode, resultCode, data));
    }

    public FlowParameters getParams() {
        return mFlowParams;
    }
}
