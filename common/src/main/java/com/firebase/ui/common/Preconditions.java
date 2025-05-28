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

package com.firebase.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Convenience class for checking argument conditions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Preconditions {

    @NonNull
    public static <T> T checkNotNull(@Nullable T o) {
        if (o == null) throw new IllegalArgumentException("Argument cannot be null.");
        return o;
    }

    public static void assertNull(@Nullable Object object, @NonNull String message) {
        if (object != null) {
            throw new RuntimeException(message);
        }
    }

    public static void assertNonNull(@Nullable Object object, @NonNull String message) {
        if (object == null) {
            throw new RuntimeException(message);
        }
    }
}
