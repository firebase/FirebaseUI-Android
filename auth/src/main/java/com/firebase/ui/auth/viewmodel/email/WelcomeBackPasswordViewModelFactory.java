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