package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.DialogFragment;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DialogBase extends DialogFragment {
    protected FragmentHelper mHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new FragmentHelper(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHelper.dismissDialog();
    }

    public void finish(int resultCode, Intent resultIntent) {
        mHelper.finish(resultCode, resultIntent);
    }
}
