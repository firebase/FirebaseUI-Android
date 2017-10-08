package com.firebase.ui.auth.util.data;

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
    protected void onInactive() {
        super.onInactive();
        mFieldResetter.run();
    }
}
