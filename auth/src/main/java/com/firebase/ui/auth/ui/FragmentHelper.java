package com.firebase.ui.auth.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.util.BaseHelper;
import com.firebase.ui.auth.util.smartlock.SignInDelegate;

public class FragmentHelper extends BaseHelper {
    private Fragment mFragment;

    public FragmentHelper(Fragment fragment, FlowParameters parameters) {
        super(fragment.getContext(), parameters);
        mFragment = fragment;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mFragment.startActivityForResult(intent, requestCode);
    }

    @Override
    public void finish(int resultCode, Intent intent) {
        if (mFragment instanceof SignInDelegate) {
            ((SignInDelegate) mFragment).finish(resultCode, intent);
        }
    }
}
