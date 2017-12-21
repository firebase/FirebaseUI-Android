package com.firebase.ui.auth.data.model;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

/**
 * Base state model object.
 * <p>
 * This state can either be successful or not. In either case, it must be done to represent these
 * states.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProgressState {
    private final boolean mComplete;
    private final Exception mException;

    /**
     * Creates a default, unfinished, state.
     */
    protected ProgressState() {
        mComplete = false;
        mException = null;
    }

    /**
     * Creates a finished state with an optional error.
     *
     * @param exception an error if one occurred
     */
    protected ProgressState(@Nullable Exception exception) {
        mComplete = true;
        mException = exception;
    }

    public final boolean isComplete() {
        return mComplete;
    }

    public final boolean isSuccessful() {
        return isComplete() && mException == null;
    }

    @Nullable
    public final Exception getException() {
        return mException;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgressState state = (ProgressState) o;

        return mComplete == state.mComplete
                && (mException == null ? state.mException == null : mException.equals(state.mException));
    }

    @Override
    public int hashCode() {
        int result = mComplete ? 1 : 0;
        result = 31 * result + (mException == null ? 0 : mException.hashCode());
        return result;
    }

    @Override
    public abstract String toString();
}
