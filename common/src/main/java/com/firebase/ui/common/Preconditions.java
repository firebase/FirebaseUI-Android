package com.firebase.ui.common;

import android.support.annotation.RestrictTo;

/**
 * Convenience class for checking argument conditions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Preconditions {

    public static <T> T checkNotNull(T o) {
        if (o == null) throw new IllegalArgumentException("Argument cannot be null.");
        return o;
    }

    public static void assertNull(Object object, String message) {
        if (object != null) {
            throw new RuntimeException(message);
        }
    }

    public static void assertNonNull(Object object, String message) {
        if (object == null) {
            throw new RuntimeException(message);
        }
    }
}
