package com.firebase.ui.auth.util.ui;

import android.app.Application;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FlowHolder extends ViewModelBase<FlowParameters> {
    private FlowParameters mFlowParams;

    public FlowHolder(Application application) {
        super(application);
    }

    @Override
    protected void onCreate(FlowParameters args) {
        mFlowParams = args;
    }

    public FlowParameters getParams() {
        return mFlowParams;
    }
}
