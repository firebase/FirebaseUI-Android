/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.firebase.ui.auth.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;

import com.firebase.ui.auth.AuthUI;

/**
 * Precondition checking utility methods.
 */
public final class Preconditions {
    private Preconditions() {
        // Helper classes shouldn't be instantiated
    }

    /**
     * Ensures that the provided value is not null, and throws a {@link NullPointerException} if it
     * is null, with a message constructed from the provided error template and arguments.
     */
    @NonNull
    public static <T> T checkNotNull(
            @Nullable T val,
            @NonNull String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        if (val == null) {
            throw new NullPointerException(String.format(errorMessageTemplate, errorMessageArgs));
        }
        return val;
    }

    /**
     * Ensures that the provided identifier matches a known style resource, and throws an {@link
     * IllegalArgumentException} if the resource cannot be found, or is not a style resource, with a
     * message constructed from the provided error template and arguments.
     */
    @StyleRes
    public static int checkValidStyle(
            @NonNull Context context,
            int styleId,
            @NonNull String errorMessageTemplate,
            @Nullable Object... errorMessageArguments) {
        try {
            String resourceType = context.getResources().getResourceTypeName(styleId);
            if (!"style".equals(resourceType)) {
                throw new IllegalArgumentException(
                        String.format(errorMessageTemplate, errorMessageArguments));
            }
            return styleId;
        } catch (Resources.NotFoundException ex) {
            throw new IllegalArgumentException(
                    String.format(errorMessageTemplate, errorMessageArguments));
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void checkUnset(@NonNull Bundle b,
                                  @Nullable String message,
                                  @NonNull String... keys) {
        for (String key : keys) {
            if (b.containsKey(key)) { throw new IllegalStateException(message); }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void checkConfigured(@NonNull Context context,
                                       @Nullable String message,
                                       @StringRes int... ids) {
        for (int id : ids) {
            if (context.getString(id).equals(AuthUI.UNCONFIGURED_CONFIG_VALUE)) {
                throw new IllegalStateException(message);
            }
        }
    }

    /**
     * Ensures the truth of an expression involving parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param errorMessage the exception message to use if the check fails
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
