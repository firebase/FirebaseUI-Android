package com.firebase.ui.database;

import android.arch.lifecycle.LifecycleObserver;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.firebase.database.DatabaseReference;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FirebaseAdapter<T> extends ChangeEventListener, LifecycleObserver {
    /**
     * Start listening for database changes and populate the adapter.
     */
    void startListening();

    /**
     * Stop listening for database changes and clear all items in the adapter.
     */
    void stopListening();

    /**
     * Returns the backing {@link ObservableSnapshotArray} used to populate this adapter.
     *
     * @return the backing snapshot array
     */
    @NonNull
    ObservableSnapshotArray<T> getSnapshots();

    /**
     * Gets the item at the specified position from the backing snapshot array.
     *
     * @see ObservableSnapshotArray#get(int)
     */
    @NonNull
    T getItem(int position);

    /**
     * Returns the reference at the specified position in this list.
     *
     * @param position index of the reference to return
     * @return the snapshot at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index
     *                                   &gt;= size()</tt>)
     */
    @NonNull
    DatabaseReference getRef(int position);
}
