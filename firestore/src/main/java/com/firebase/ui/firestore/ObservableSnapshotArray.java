package com.firebase.ui.firestore;

import android.support.annotation.NonNull;

import com.firebase.ui.common.BaseObservableSnapshotArray;
import com.firebase.ui.common.BaseSnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Subclass of {@link BaseObservableSnapshotArray} for Firestore data.
 */
public abstract class ObservableSnapshotArray<T>
        extends BaseObservableSnapshotArray<DocumentSnapshot, FirebaseFirestoreException, ChangeEventListener, T> {

    public ObservableSnapshotArray() {
        super();
    }

    /**
     * See {@link BaseObservableSnapshotArray#BaseObservableSnapshotArray(BaseSnapshotParser)}
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<T> parser) {
        super(parser);
    }
}
