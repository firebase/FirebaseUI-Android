package com.firebase.ui.common;

import android.arch.lifecycle.LifecycleObserver;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Adapter<O extends BaseObservableSnapshotArray<S, E, ?, T>, S, E, T>
        extends BaseChangeEventListener<S, E>, LifecycleObserver {
    /**
     * Start listening for database changes and populate the adapter.
     */
    void startListening();

    /**
     * Stop listening for database changes and clear all items in the adapter.
     */
    void stopListening();

    /**
     * Returns the backing {@link BaseObservableSnapshotArray} used to populate this adapter.
     *
     * @return the backing snapshot array
     */
    @NonNull
    O getSnapshots();

    /**
     * Gets the item at the specified position from the backing snapshot array.
     *
     * @see BaseObservableSnapshotArray#get(int)
     */
    @NonNull
    T getItem(int position);
}
