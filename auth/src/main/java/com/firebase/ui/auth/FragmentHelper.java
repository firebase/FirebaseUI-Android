package com.firebase.ui.auth;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.util.BaseHelper;

public class FragmentHelper extends BaseHelper {
    private Fragment mFragment;

    public FragmentHelper(Fragment fragment) {
        super(fragment.getContext(), (FlowParameters) fragment.getArguments()
                .getParcelable(ExtraConstants.EXTRA_FLOW_PARAMS));
        mFragment = fragment;
    }

    public static Bundle getFlowParamsBundle(FlowParameters params) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, params);
        return bundle;
    }

    public void startIntentSenderForResult(IntentSender sender, int requestCode)
            throws IntentSender.SendIntentException {
        mFragment.startIntentSenderForResult(sender, requestCode, null, 0, 0, 0, null);
    }
}
