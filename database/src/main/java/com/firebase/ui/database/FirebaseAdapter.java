package com.firebase.ui.database;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.common.Adapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FirebaseAdapter<T> extends Adapter<ObservableSnapshotArray<T>,
        DataSnapshot, DatabaseError,
        T>, ChangeEventListener {
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
