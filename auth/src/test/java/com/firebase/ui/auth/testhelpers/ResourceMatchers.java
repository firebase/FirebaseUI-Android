package com.firebase.ui.auth.testhelpers;

import com.firebase.ui.auth.FirebaseUiException;
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

    public static <T> ArgumentMatcher<Resource<T>> isFailureWithCode(final int code) {
        return argument -> {
            if (argument.getState() != State.FAILURE) {
                return false;
            }

            if (argument.getException() == null) {
                return false;
            }

            if (!(argument.getException() instanceof FirebaseUiException)) {
                return false;
            }

            FirebaseUiException fue = (FirebaseUiException) argument.getException();
            return fue.getErrorCode() == code;
        };
    }

    public static <T> ArgumentMatcher<Resource<T>> isSuccessWith(final T result) {
        return argument -> argument.getState() == State.SUCCESS
                && argument.getValue().equals(result);
    }

    private static <T> ArgumentMatcher<Resource<T>> isState(final State state) {
        return argument -> argument.getState() == state;
    }

}
