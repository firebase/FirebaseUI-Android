package com.firebase.ui.auth.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.util.BaseHelper;

/**
 * A simple Fragment with {@code setRetainInstance} set to true.
 */
public class BaseFragment extends Fragment {
    protected BaseHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mHelper = new BaseHelper(getContext(),
                                 (FlowParameters) getArguments()
                                         .getParcelable(ExtraConstants.EXTRA_FLOW_PARAMS));
    }
}
