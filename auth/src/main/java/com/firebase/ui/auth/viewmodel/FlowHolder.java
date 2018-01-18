package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.data.model.FlowParameters;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FlowHolder extends ViewModelBase<FlowParameters> {
    public FlowHolder(Application application) {
        super(application);
    }

    @Override
    public FlowParameters getArguments() {
        return super.getArguments();
    }
}
