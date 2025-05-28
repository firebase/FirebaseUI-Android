/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.viewmodel;

import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.ProgressView;
import com.firebase.ui.auth.util.ui.FlowUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.lifecycle.Observer;

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
