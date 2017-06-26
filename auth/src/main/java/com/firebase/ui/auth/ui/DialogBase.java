package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DialogBase extends DialogFragment {

    protected FragmentHelper mHelper;
    protected ProgressDialogHolder mProgressDialogHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new FragmentHelper(this);

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
        mHelper.finish(resultCode, resultIntent);
    }
}
