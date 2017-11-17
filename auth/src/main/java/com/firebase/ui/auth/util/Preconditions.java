package com.firebase.ui.auth.util;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.StyleRes;

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
     * Ensures that the provided identifier matches a known style resource, and throws an {@link
     * IllegalArgumentException} if the resource cannot be found, or is not a style resource, with a
     * message constructed from the provided error template and arguments.
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
