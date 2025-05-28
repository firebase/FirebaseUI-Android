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
