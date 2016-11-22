package com.firebase.ui.auth.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.util.BaseHelper;

public class BaseFragment extends Fragment {
    protected BaseHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new BaseHelper(getContext(), (FlowParameters) getArguments()
                .getParcelable(ExtraConstants.EXTRA_FLOW_PARAMS));
    }

    public static Bundle getFlowParamsBundle(FlowParameters params) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, params);
        return bundle;
    }
}
