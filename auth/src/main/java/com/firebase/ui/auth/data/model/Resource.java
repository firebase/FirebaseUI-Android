/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

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

    private boolean mIsUsed;

    private Resource(State state, T value, Exception exception) {
        mState = state;
        mValue = value;
        mException = exception;
    }

    /**
     * Creates a successful resource containing a value.
     */
    @NonNull
    public static <T> Resource<T> forSuccess(@NonNull T value) {
        return new Resource<>(State.SUCCESS, value, null);
    }

    /**
     * Creates a failed resource with an exception.
     */
    @NonNull
    public static <T> Resource<T> forFailure(@NonNull Exception e) {
        return new Resource<>(State.FAILURE, null, e);
    }

    /**
     * Creates a resource in the loading state, without a value or an exception.
     */
    @NonNull
    public static <T> Resource<T> forLoading() {
        return new Resource<>(State.LOADING, null, null);
    }

    @NonNull
    public State getState() {
        return mState;
    }

    @Nullable
    public final Exception getException() {
        mIsUsed = true;
        return mException;
    }

    @Nullable
    public T getValue() {
        mIsUsed = true;
        return mValue;
    }

    public boolean isUsed() {
        return mIsUsed;
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
