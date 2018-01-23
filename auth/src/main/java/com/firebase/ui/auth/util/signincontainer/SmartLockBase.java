package com.firebase.ui.auth.util.signincontainer;

import android.content.Intent;
import android.util.Pair;

import com.firebase.ui.auth.ui.FragmentBase;

public abstract class SmartLockBase extends FragmentBase {

    private boolean mWasProgressDialogShowing;
    private Pair<Integer, Intent> mActivityResultPair;

    @Override
    public void onStart() {
        super.onStart();
        if (mActivityResultPair != null) {
            finish(mActivityResultPair.first, mActivityResultPair.second);
        } else if (mWasProgressDialogShowing) {
            getDialogHolder().showLoadingDialog(com.firebase.ui.auth.R.string.fui_progress_dialog_loading);
            mWasProgressDialogShowing = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mWasProgressDialogShowing = getDialogHolder().isProgressDialogShowing();
        getDialogHolder().dismissDialog();
    }

    @Override
    public void finish(int resultCode, Intent resultIntent) {
        if (getActivity() == null) {
            // Because this fragment lives beyond the activity lifecycle, Fragment#getActivity()
            // might return null and we'll throw a NPE. To get around this, we wait until the
            // activity comes back to life in onStart and we finish it there.
            mActivityResultPair = new Pair<>(resultCode, resultIntent);
        } else {
            super.finish(resultCode, resultIntent);
        }
    }
}
