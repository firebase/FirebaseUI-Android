package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.CallSuper;
import android.support.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicBoolean;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ViewModelBase<T> extends AndroidViewModel {
    private final AtomicBoolean mIsInitialized = new AtomicBoolean();

    private T mArguments;

    protected ViewModelBase(Application application) {
        super(application);
    }

    public void init(T args) {
        if (mIsInitialized.compareAndSet(false, true)) {
            mArguments = args;
            onCreate();
        }
    }

    protected void onCreate() {}

    protected T getArguments() {
        return mArguments;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    protected void setArguments(T arguments) {
        mArguments = arguments;
    }

    @CallSuper
    @Override
    protected void onCleared() {
        mIsInitialized.set(false);
    }
}
