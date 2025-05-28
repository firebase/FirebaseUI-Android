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

package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.ui.auth.data.model.FlowParameters;

/**
 * Factory for creating WelcomeBackPasswordViewModel instances.
 */
public class WelcomeBackPasswordViewModelFactory implements ViewModelProvider.Factory {
    private final Application mApplication;
    private final FlowParameters mFlowParams;

    public WelcomeBackPasswordViewModelFactory(Application application, FlowParameters flowParams) {
        mApplication = application;
        mFlowParams = flowParams;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass == WelcomeBackPasswordViewModel.class) {
            WelcomeBackPasswordViewModel viewModel = new WelcomeBackPasswordViewModel(mApplication);
            viewModel.init(mFlowParams);
            return (T) viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
