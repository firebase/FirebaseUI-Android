package com.firebase.ui.auth.data.model;

import android.support.annotation.Nullable;

public abstract class ProgressState {
    private final boolean mIsDone;
    private final Exception mException;

    protected ProgressState(boolean done, @Nullable Exception exception) {
        mIsDone = done;
        mException = exception;
    }

    public final boolean isDone() {
        return mIsDone;
    }

    public final boolean isSuccessful() {
        return isDone() && mException == null;
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

        return mIsDone == state.mIsDone
                && (mException == null ? state.mException == null : mException.equals(state.mException));
    }

    @Override
    public int hashCode() {
        int result = mIsDone ? 1 : 0;
        result = 31 * result + (mException != null ? mException.hashCode() : 0);
        return result;
    }

    @Override
    public abstract String toString();
}
