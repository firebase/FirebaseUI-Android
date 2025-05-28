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
