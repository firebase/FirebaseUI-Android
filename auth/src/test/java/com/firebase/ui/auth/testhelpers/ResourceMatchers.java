package com.firebase.ui.auth.testhelpers;

import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.State;

import org.mockito.ArgumentMatcher;

/**
 * Utilities for testing {@link Resource}.
 */
public class ResourceMatchers {

    public static <T> ArgumentMatcher<Resource<T>> isLoading() {
        return isState(State.LOADING);
    }

    public static <T> ArgumentMatcher<Resource<T>> isSuccess() {
        return isState(State.SUCCESS);
    }

    public static <T> ArgumentMatcher<Resource<T>> isFailure() {
        return isState(State.FAILURE);
    }

    public static <T> ArgumentMatcher<Resource<T>> isSuccessWith(final T result) {
        return new ArgumentMatcher<Resource<T>>() {
            @Override
            public boolean matches(Resource<T> argument) {
                return argument.getState() == State.SUCCESS
                        && argument.getValue().equals(result);
            }
        };
    }

    private static <T> ArgumentMatcher<Resource<T>> isState(final State state) {
        return new ArgumentMatcher<Resource<T>>() {
            @Override
            public boolean matches(Resource<T> argument) {
                return argument.getState() == state;
            }
        };
    }

}
