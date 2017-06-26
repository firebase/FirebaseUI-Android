package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FragmentBase extends Fragment {

    protected BaseHelper mHelper;
    protected ProgressDialogHolder mProgressDialogHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new BaseHelper(this, this.getArguments());

        ContextThemeWrapper context = new ContextThemeWrapper(
                getContext(), mHelper.getFlowParams().themeId);
        mProgressDialogHolder = new ProgressDialogHolder(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgressDialogHolder.dismissDialog();
    }

    public void finish(int resultCode, Intent resultIntent) {
        BaseHelper.finishActivity(getActivity(), resultCode, resultIntent);
    }

    public void startIntentSenderForResult(IntentSender sender, int requestCode)
            throws IntentSender.SendIntentException {
        startIntentSenderForResult(sender, requestCode, null, 0, 0, 0, null);
    }
}
