package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FragmentHelper extends BaseHelper {

    private Fragment mFragment;

    public FragmentHelper(Fragment fragment) {
        super(fragment.getContext().getApplicationContext(),
              (FlowParameters) fragment.getArguments().getParcelable(ExtraConstants.EXTRA_FLOW_PARAMS));
        mFragment = fragment;
    }

    public void finish(int resultCode, Intent intent) {
        finishActivity(mFragment.getActivity(), resultCode, intent);
    }

    public void startIntentSenderForResult(IntentSender sender, int requestCode)
            throws IntentSender.SendIntentException {
        mFragment.startIntentSenderForResult(sender, requestCode, null, 0, 0, 0, null);
    }
}
