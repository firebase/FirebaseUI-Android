package com.firebase.ui.database;

/**
 * TODO(samstern): Document
 */
public class Preconditions {

    public static void checkNotNull(Object o) {
        if (o == null) throw new IllegalArgumentException("Listener cannot be null.");
    }

}
