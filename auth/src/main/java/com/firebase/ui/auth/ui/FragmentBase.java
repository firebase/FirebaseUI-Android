package com.firebase.ui.auth.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;

import com.firebase.ui.auth.util.FlowHolder;
import com.firebase.ui.auth.util.SignInHandler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FragmentBase extends Fragment {

    private FlowHolder mFlowHolder;
    private SignInHandler mSignInHandler;

    private ProgressDialogHolder mProgressDialogHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFlowHolder = ViewModelProviders.of(getActivity()).get(FlowHolder.class);
        mSignInHandler = ViewModelProviders.of(getActivity()).get(SignInHandler.class);
        mProgressDialogHolder = new ProgressDialogHolder(new ContextThemeWrapper(
                getContext(), getFlowHolder().getParams().themeId));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    public FlowHolder getFlowHolder() {
        return mFlowHolder;
    }

    public SignInHandler getSignInHandler() {
        return mSignInHandler;
    }

    public ProgressDialogHolder getDialogHolder() {
        return mProgressDialogHolder;
    }

    public void finish(int resultCode, Intent resultIntent) {
        getActivity().setResult(resultCode, resultIntent);
        getActivity().finish();
    }

    public void startIntentSenderForResult(IntentSender sender, int requestCode)
            throws IntentSender.SendIntentException {
        startIntentSenderForResult(sender, requestCode, null, 0, 0, 0, null);
    }
}
