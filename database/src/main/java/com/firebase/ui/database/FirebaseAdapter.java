package com.firebase.ui.database;

import com.google.firebase.database.DatabaseReference;

public interface FirebaseAdapter<T> extends ChangeEventListener, SnapshotParser<T> {
    /**
     * If you need to do some setup before the adapter starts listening for change events in the
     * database (such as setting a custom {@link JoinResolver}), do so it here and then call {@code
     * super.startListening()}.
     */
    void startListening();

    /**
     * Removes listeners and clears all items in the backing {@link FirebaseArray}.
     */
    void cleanup();

    T getItem(int position);

    DatabaseReference getRef(int position);
}
