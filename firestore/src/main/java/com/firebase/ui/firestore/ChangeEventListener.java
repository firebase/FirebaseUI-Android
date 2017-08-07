package com.firebase.ui.firestore;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Listener for changes to a {@link FirestoreArray}.
 * TODO: This could be a common interface, it just needs to know about the snapshot and error types.
 */
public interface ChangeEventListener {

    /**
     * The type of change to an element of the array,
     */
    enum Type {

        /**
         * An element was added to the array.
         */
        ADDED,

        /**
         * An element was removed from the array.
         */
        REMOVED,

        /**
         * An element in the array has new content.
         */
        MODIFIED,

        /**
         * An element in the array has a new position, and also new content.
         */
        MOVED
    }

    /**
     * A callback for when a child event occurs in a FirestoreArray.
     * @param type      The type of the event.
     * @param snapshot  The {@link DocumentSnapshot} of the changed child.
     * @param newIndex  The new index of the element, or -1 of it is no longer
     * @param oldIndex  The previous index of the element, or -1 if it was not
*                  previously tracked.
     */
    void onChildChanged(Type type, DocumentSnapshot snapshot,
                        int newIndex, int oldIndex);

    /**
     * Callback triggered after all child events in a particular snapshot have been
     * processed.
     * <p>
     * Useful for batch events, such as removing a loading indicator after initial load
     * or a large update batch.
     */
    void onDataChanged();

    /**
     * Callback when an error has been detected in the underlying Firestore query listener.
     * @param e the error that occurred.
     */
    void onError(FirebaseFirestoreException e);

}
