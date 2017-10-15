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
 * A LiveData that only notifies observers when new data arrives, used for events like navigation
 * and SnackBar messages.
 * <p>
 * This avoids a common problem with events: on configuration change (like rotation) an update can
 * be emitted if the observer is active.
 * <p>
 * This LiveData supports multiple observers per class definition, but works on a stack model. Only
 * the newest observers get updates.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {
    private final Map<Class<? extends Observer>, AtomicBoolean> mObserverStatuses = new ConcurrentHashMap<>();
    private final List<EventFilterObserver<T>> mObservers = new ArrayList<>();

    @Override
    public void observe(LifecycleOwner owner, Observer<T> observer) {
        if (!mObserverStatuses.containsKey(observer.getClass())) {
            mObserverStatuses.put(observer.getClass(), new AtomicBoolean());
        }

        EventFilterObserver<T> filterObserver = new EventFilterObserver<>(observer);
        mObservers.add(0, filterObserver);
        super.observe(owner, filterObserver);
    }

    @Override
    public void removeObserver(Observer<T> observer) {
        EventFilterObserver<T> filterObserver;
        if (observer instanceof EventFilterObserver) {
            filterObserver = (EventFilterObserver<T>) observer;
        } else {
            filterObserver = null;
            for (EventFilterObserver<T> activeObserver : mObservers) {
                Observer<T> originalObserver = activeObserver.getOriginalObserver();
                if (originalObserver == observer) {
                    filterObserver = activeObserver;
                }
            }

            if (filterObserver == null) {
                throw new IllegalStateException("Observer added without filter");
            }
        }
        mObservers.remove(filterObserver);
        super.removeObserver(filterObserver);
    }

    @Override
    public void setValue(@Nullable T t) {
        for (AtomicBoolean aBoolean : mObserverStatuses.values()) {
            aBoolean.set(true);
        }
        super.setValue(t);
    }

    public boolean isObserving(Class<? extends Observer> observerClass) {
        for (EventFilterObserver<T> filterObserver : mObservers) {
            if (observerClass == filterObserver.getOriginalObserver().getClass()) {
                return true;
            }
        }

        return false;
    }

    private class EventFilterObserver<T2> implements Observer<T2> {
        private final Observer<T2> mObserver;

        public EventFilterObserver(Observer<T2> observer) {
            mObserver = observer;
        }

        public Observer<T2> getOriginalObserver() {
            return mObserver;
        }

        @Override
        public void onChanged(@Nullable T2 t) {
            if (mObserverStatuses.get(mObserver.getClass()).compareAndSet(true, false)) {
                getNewestObserver().onChanged(t);
            }
        }

        private Observer<T2> getNewestObserver() {
            for (EventFilterObserver<T> observer : mObservers) {
                Observer<T> originalObserver = observer.getOriginalObserver();
                if (originalObserver.getClass() == mObserver.getClass()) {
                    //noinspection unchecked
                    return (Observer<T2>) originalObserver;
                }
            }

            throw new IllegalStateException("Received update for non-existent observer");
        }
    }
}