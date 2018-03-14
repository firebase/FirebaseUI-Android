package com.firebase.ui.auth.util;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;

import java.lang.ref.WeakReference;

/**
 * Failure listener for use with AnonymousUpgradeUtils.
 */
public abstract class UpgradeFailureListener implements OnFailureListener {

    private final WeakReference<HelperActivityBase> mActivity;
    private final FlowParameters mParameters;
    private final FirebaseAuth mAuth;
    private final AuthCredential mCredential;

    public UpgradeFailureListener(HelperActivityBase activity,
                                  AuthCredential credential) {
        mActivity = new WeakReference<>(activity);

        mParameters = activity.getFlowParams();
        mAuth = activity.getFirebaseAuth();
        mCredential = credential;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (AnonymousUpgradeUtils.isUpgradeFailure(mParameters, mAuth, e)) {
            IdpResponse response = new IdpResponse.Builder(mCredential).build();
            if (mActivity.get() != null) {
                mActivity.get().finish(Activity.RESULT_CANCELED, response.toIntent());
            }
        } else {
            onNonUpgradeFailure(e);
        }
    }

    public abstract void onNonUpgradeFailure(@NonNull Exception e);
}
