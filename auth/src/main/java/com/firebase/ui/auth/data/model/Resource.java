package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.util.Preconditions;

/**
 * Base state model object.
 * <p>
 * This state can either be successful or not. In either case, it must be complete to represent
 * these states.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Resource<T> {
    private final State mState;
    private final T mValue;
    private final Exception mException;

    /**
     * Creates a default, unfinished, state.
     */
    public Resource() {
        mState = State.LOADING;
        mValue = null;
        mException = null;
    }

    /**
     * Creates a successful Resource&lt;Void&gt;.
     */
    public static Resource<Void> forVoidSuccess() {
        return new Resource<>(State.SUCCESS, null, null);
    }

    /**
     * Creates a finished success state.
     *
     * @param value result of the operation
     */
    public Resource(@NonNull T value) {
        mState = State.SUCCESS;
        mValue = Preconditions.checkNotNull(value, "Success state cannot have null result.");
        mException = null;
    }

    /**
     * Creates a finished failure state.
     *
     * @param exception error in computing the result
     */
    public Resource(@NonNull Exception exception) {
        mState = State.FAILURE;
        mValue = null;
        mException = Preconditions.checkNotNull(exception, "Failure state cannot have null error.");
    }

    private Resource(State state, T value, Exception exception) {
        mState = state;
        mValue = value;
        mException = exception;
    }

    @NonNull
    public State getState() {
        return mState;
    }

    @Nullable
    public final Exception getException() {
        return mException;
    }

    @Nullable
    public T getValue() {
        return mValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource<?> resource = (Resource<?>) o;

        return mState == resource.mState
                && (mValue == null ? resource.mValue == null : mValue.equals(resource.mValue))
                && (mException == null ? resource.mException == null : mException.equals(resource.mException));
    }

    @Override
    public int hashCode() {
        int result = mState.hashCode();
        result = 31 * result + (mValue == null ? 0 : mValue.hashCode());
        result = 31 * result + (mException == null ? 0 : mException.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "mState=" + mState +
                ", mValue=" + mValue +
                ", mException=" + mException +
                '}';
    }
}
