package com.firebase.ui.auth.util;

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and SnackBar messages.
 * <p>
 * This avoids a common problem with events: on configuration change (like rotation) an update can
 * be emitted if the observer is active.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {
    private final Map<Class<?>, AtomicBoolean> mObservers = new ConcurrentHashMap<>();

    @Override
    public void observe(LifecycleOwner owner, final Observer<T> observer) {
        if (!mObservers.containsKey(observer.getClass())) {
            mObservers.put(observer.getClass(), new AtomicBoolean());
        }

        super.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                if (mObservers.get(observer.getClass()).compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            }
        });
        owner.getLifecycle().addObserver(new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    mObservers.remove(observer.getClass());
                }
            }
        });
    }

    @Override
    public void setValue(@Nullable T t) {
        for (AtomicBoolean aBoolean : mObservers.values()) {
            aBoolean.set(true);
        }
        super.setValue(t);
    }
}
