package com.firebase.ui.firestore;

import android.support.annotation.NonNull;

import com.firebase.ui.common.BaseObservableSnapshotArray;
import com.firebase.ui.common.BaseSnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Subclass of {@link BaseObservableSnapshotArray} for Firestore data.
 */
public abstract class ObservableSnapshotArray<E>
        extends BaseObservableSnapshotArray<DocumentSnapshot, ChangeEventListener, E> {

    public ObservableSnapshotArray() {
        super();
    }

    /**
     * See {@link BaseObservableSnapshotArray#BaseObservableSnapshotArray(BaseSnapshotParser)}
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<E> parser) {
        super(parser);
    }

    // TODO: There should be a way to move this into the base class
    protected void notifyOnError(FirebaseFirestoreException e) {
        for (ChangeEventListener listener : getListeners()) {
            listener.onError(e);
        }
    }
}
