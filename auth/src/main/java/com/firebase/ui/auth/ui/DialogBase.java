package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DialogBase extends DialogFragment {

    protected FlowParameters mFlowParameters;
    protected ProgressDialogHolder mProgressDialogHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContextThemeWrapper context = new ContextThemeWrapper(
                getContext(), getFlowParams().themeId);
        mProgressDialogHolder = new ProgressDialogHolder(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    public FlowParameters getFlowParams() {
        if (mFlowParameters == null) {
            mFlowParameters = FlowParameters.fromBundle(getArguments());
        }

        return mFlowParameters;
    }

    public void finish(int resultCode, Intent resultIntent) {
        getActivity().setResult(resultCode, resultIntent);
        getActivity().finish();
    }
}
