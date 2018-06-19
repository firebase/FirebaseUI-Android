package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.ProgressView;
import com.firebase.ui.auth.util.ui.FlowUtils;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ResourceObserver<T> implements Observer<Resource<T>> {

    private final ProgressView mProgressView;
    private final HelperActivityBase mActivity;
    private final FragmentBase mFragment;
    private final int mLoadingMessage;

    protected ResourceObserver(@NonNull HelperActivityBase activity) {
        this(activity, null, activity, R.string.fui_progress_dialog_loading);
    }

    protected ResourceObserver(@NonNull HelperActivityBase activity, @StringRes int message) {
        this(activity, null, activity, message);
    }

    protected ResourceObserver(@NonNull FragmentBase fragment) {
        this(null, fragment, fragment, R.string.fui_progress_dialog_loading);
    }

    protected ResourceObserver(@NonNull FragmentBase fragment, @StringRes int message) {
        this(null, fragment, fragment, message);
    }

    private ResourceObserver(HelperActivityBase activity,
                             FragmentBase fragment,
                             ProgressView progressView,
                             int message) {
        mActivity = activity;
        mFragment = fragment;

        if (mActivity == null && mFragment == null) {
            throw new IllegalStateException("ResourceObserver must be attached to an Activity or a Fragment");
        }

        mProgressView = progressView;
        mLoadingMessage = message;
    }

    @Override
    public final void onChanged(Resource<T> resource) {
        if (resource.getState() == State.LOADING) {
            mProgressView.showProgress(mLoadingMessage);
            return;
        }
        mProgressView.hideProgress();

        if (resource.isUsed()) { return; }

        if (resource.getState() == State.SUCCESS) {
            onSuccess(resource.getValue());
        } else if (resource.getState() == State.FAILURE) {
            Exception e = resource.getException();
            boolean unhandled;
            if (mFragment == null) {
                unhandled = FlowUtils.unhandled(mActivity, e);
            } else {
                unhandled = FlowUtils.unhandled(mFragment, e);
            }
            if (unhandled) {
                Log.e(AuthUI.TAG, "A sign-in error occurred.", e);
                onFailure(e);
            }
        }
    }

    protected abstract void onSuccess(@NonNull T t);

    protected abstract void onFailure(@NonNull Exception e);
}
