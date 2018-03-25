package com.firebase.ui.auth.viewmodel;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.ui.FlowUtils;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ResourceObserver<T> implements Observer<Resource<T>> {
    private final HelperActivityBase mActivity;
    private final FragmentBase mFragment;
    private final int mLoadingMessage;

    protected ResourceObserver(@NonNull HelperActivityBase activity, @StringRes int message) {
        this(activity, null, message);
    }

    protected ResourceObserver(@NonNull FragmentBase fragment, @StringRes int message) {
        this((HelperActivityBase) fragment.getActivity(), fragment, message);
    }

    private ResourceObserver(HelperActivityBase activity, FragmentBase fragment, int message) {
        mActivity = activity;
        mFragment = fragment;
        mLoadingMessage = message;
    }

    @Override
    public final void onChanged(Resource<T> resource) {
        if (resource.getState() == State.LOADING) {
            mActivity.getDialogHolder().showLoadingDialog(mLoadingMessage);
            return;
        }
        mActivity.getDialogHolder().dismissDialog();

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
            if (unhandled) { onFailure(e); }
        }
    }

    protected abstract void onSuccess(@NonNull T t);

    protected abstract void onFailure(@NonNull Exception e);
}
