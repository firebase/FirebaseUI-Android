package com.firebase.ui.firestore;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * TODO
 */
public interface ChangeEventListener {

    // TODO: add oldIndex if necessary
    void onChildChanged(DocumentChange.Type eventType, DocumentSnapshot snapshot, int index);

    void onDataChanged();

    void onError(FirebaseFirestoreException e);

}
