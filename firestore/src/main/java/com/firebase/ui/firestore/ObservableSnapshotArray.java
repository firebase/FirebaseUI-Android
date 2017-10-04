package com.firebase.ui.firestore;

import android.support.annotation.NonNull;

import com.firebase.ui.common.BaseCachingSnapshotParser;
import com.firebase.ui.common.BaseObservableSnapshotArray;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Subclass of {@link BaseObservableSnapshotArray} for Firestore data.
 */
public abstract class ObservableSnapshotArray<T>
        extends BaseObservableSnapshotArray<DocumentSnapshot, FirebaseFirestoreException, ChangeEventListener, T> {
    /**
     * @see BaseObservableSnapshotArray#BaseObservableSnapshotArray(BaseCachingSnapshotParser)
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<T> parser) {
        super(new CachingSnapshotParser<>(parser));
    }
}
