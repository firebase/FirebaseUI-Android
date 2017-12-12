package com.firebase.ui.auth.util.ui;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ViewModelBase<T> extends AndroidViewModel {
    private AtomicBoolean mIsInitialized = new AtomicBoolean();

    protected ViewModelBase(Application application) {
        super(application);
    }

    public void init(T args) {
        if (mIsInitialized.compareAndSet(false, true)) {
            onCreate(args);
        }
    }

    protected abstract void onCreate(T args);

    @Override
    protected void onCleared() {
        mIsInitialized.set(false);
    }
}
