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
import android.support.annotation.StyleRes;

/**
 * Precondition checking utility methods.
 */
public final class Preconditions {

    /**
     * Ensures that the provided value is not null, and throws a {@link NullPointerException}
     * if it is null, with a message constructed from the provided error template and arguments.
     */
    public static <T> T checkNotNull(
            T val,
            String errorMessageTemplate,
            Object... errorMessageArgs) {
        if (val == null) {
            throw new NullPointerException(String.format(errorMessageTemplate, errorMessageArgs));
        }
        return val;
    }

    /**
     * Ensures that the provided identifier matches a known style resource, and throws
     * an {@link IllegalArgumentException} if the resource cannot be found, or is not
     * a style resource, with a message constructed from the provided error template and arguments.
     */
    @StyleRes
    public static int checkValidStyle(
            Context context,
            int styleId,
            String errorMessageTemplate,
            Object... errorMessageArguments) {
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
}
