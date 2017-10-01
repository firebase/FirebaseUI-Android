package com.firebase.ui.auth.util;

import android.arch.lifecycle.ViewModel;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ViewModelBase<T> extends ViewModel {
    private AtomicBoolean mIsInitialized = new AtomicBoolean();

    public void init(T args) {
        if (!mIsInitialized.get()) {
            onCreate(args);
            mIsInitialized.set(true);
        }
    }

    protected abstract void onCreate(T args);

    @Override
    protected void onCleared() {
        mIsInitialized.set(false);
    }
}
