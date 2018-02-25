package com.firebase.ui.auth.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.viewmodel.FlowHolder;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FragmentBase extends Fragment {
    private FlowHolder mFlowHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFlowHolder = ViewModelProviders.of(getActivity()).get(FlowHolder.class);
    }

    public FlowParameters getFlowParams() {
        return mFlowHolder.getArguments();
    }
}
