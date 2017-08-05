package com.firebase.ui.firestore;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * TODO: This could be a common interface, it just needs to know about the snapshot and error types.
 */
public interface ChangeEventListener {

    enum Type {
        ADDED,
        REMOVED,
        MODIFIED,
        MOVED
    }

    void onChildChanged(Type type, DocumentSnapshot snapshot,
                        int oldIndex, int newIndex);

    void onDataChanged();

    void onError(FirebaseFirestoreException e);

}
