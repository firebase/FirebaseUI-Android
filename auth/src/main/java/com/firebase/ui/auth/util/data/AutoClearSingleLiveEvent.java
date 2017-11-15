package com.firebase.ui.auth.util.data;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;

/**
 * LiveData that resets itself when all listeners are removed. Used to support fields which need to
 * be static to support previous activities in the stack dying, but really shouldn't be static.
 */
public class AutoClearSingleLiveEvent<T> extends SingleLiveEvent<T> {
    private final Runnable mFieldResetter;

    public AutoClearSingleLiveEvent(Runnable resetter) {
        mFieldResetter = resetter;
    }

    @Override
    public void removeObserver(@NonNull Observer<T> observer) {
        super.removeObserver(observer);
        if (!hasObservers()) {
            mFieldResetter.run();
        }
    }
}
