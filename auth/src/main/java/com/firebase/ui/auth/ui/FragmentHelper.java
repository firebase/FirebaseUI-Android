package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.ui.idp.EmailHintContainer;
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity;
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
        if (mFragment instanceof EmailHintContainer) {
            if (mFragment.getActivity() instanceof AuthMethodPickerActivity) {
                if (resultCode == Activity.RESULT_OK) {
                    ((AuthMethodPickerActivity) mFragment.getActivity()).finish(Activity.RESULT_OK, intent);
                }
            } else {
                SignInDelegate signInDelegate = (SignInDelegate) mFragment.getActivity()
                        .getSupportFragmentManager()
                        .findFragmentByTag(SignInDelegate.TAG);
                signInDelegate.finish(resultCode, intent);
            }
        }
    }
}
