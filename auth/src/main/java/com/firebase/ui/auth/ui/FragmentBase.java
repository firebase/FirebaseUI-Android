package com.firebase.ui.auth.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.firebase.auth.FirebaseUser;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FragmentBase extends Fragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public FlowParameters getFlowParams() {
        return ((HelperActivityBase) getActivity()).getFlowParams();
    }

    public void startSaveCredentials(
            FirebaseUser firebaseUser,
            @Nullable String password,
            IdpResponse response) {
        ((HelperActivityBase) getActivity()).startSaveCredentials(firebaseUser, response, password);
    }
}
