package com.firebase.ui.auth.ui;

import android.os.Bundle;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class FragmentBase extends Fragment implements ProgressView {
    private HelperActivityBase mActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();
        if (!(activity instanceof HelperActivityBase)) {
            throw new IllegalStateException("Cannot use this fragment without the helper activity");
        }
        mActivity = (HelperActivityBase) activity;
    }

    public FlowParameters getFlowParams() {
        return mActivity.getFlowParams();
    }

    public void startSaveCredentials(
            FirebaseUser firebaseUser,
            IdpResponse response,
            @Nullable String password) {
        mActivity.startSaveCredentials(firebaseUser, response, password);
    }
}
