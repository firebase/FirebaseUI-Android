package com.firebase.ui.common;

import android.support.annotation.NonNull;

/**
 * Event listener for changes in an {@link BaseObservableSnapshotArray}.
 */
public interface BaseChangeEventListener<S, E> {

    /**
     * A callback for when a child event occurs.
     *
     * @param type      The type of the event.
     * @param snapshot  The snapshot of the changed child.
     * @param newIndex  The new index of the element, or -1 of it is no longer present
     * @param oldIndex  The previous index of the element, or -1 if it was not
     *                  previously tracked.
     */
    void onChildChanged(
            @NonNull ChangeEventType type, @NonNull S snapshot, int newIndex, int oldIndex);

    /**
     * Callback triggered after all child events in a particular snapshot have been
     * processed.
     * <p>
     * Useful for batch events, such as removing a loading indicator after initial load
     * or a large update batch.
     */
    void onDataChanged();

    /**
     * Callback when an error has been detected in the underlying listener.
     * @param e the error that occurred.
     */
    void onError(@NonNull E e);

}
