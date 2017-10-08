package com.firebase.ui.auth.util.data;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
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
    private final Map<Class<? extends Observer>, AtomicBoolean> mObserverStatuses = new ConcurrentHashMap<>();
    private final List<Class<? extends Observer>> mActiveObservers = new ArrayList<>();

    @Override
    public void observe(LifecycleOwner owner, final Observer<T> observer) {
        final Class<? extends Observer> observerClass = observer.getClass();

        if (mActiveObservers.contains(observerClass)) {
            throw new IllegalStateException(
                    "Cannot add multiple observer instances for " + observerClass);
        } else {
            mActiveObservers.add(observerClass);
        }
        if (!mObserverStatuses.containsKey(observerClass)) {
            mObserverStatuses.put(observerClass, new AtomicBoolean());
        }

        super.observe(owner, new EventFilterObserver<>(observer));
    }

    @Override
    public void removeObserver(Observer<T> observer) {
        super.removeObserver(observer);
        Class<? extends  Observer> observerClass;
        if (observer instanceof EventFilterObserver) {
            observerClass = ((EventFilterObserver) observer).getOriginalObserver().getClass();
        } else {
            observerClass = observer.getClass();
        }
        mActiveObservers.remove(observerClass);
    }

    @Override
    public void setValue(@Nullable T t) {
        for (AtomicBoolean aBoolean : mObserverStatuses.values()) {
            aBoolean.set(true);
        }
        super.setValue(t);
    }

    private class EventFilterObserver<T> implements Observer<T> {
        private final Observer<T> mObserver;

        public EventFilterObserver(Observer<T> observer) {
            mObserver = observer;
        }

        private Observer<T> getOriginalObserver() {
            return mObserver;
        }

        @Override
        public void onChanged(@Nullable T t) {
            if (mObserverStatuses.get(mObserver.getClass()).compareAndSet(true, false)) {
                mObserver.onChanged(t);
            }
        }
    }
}
