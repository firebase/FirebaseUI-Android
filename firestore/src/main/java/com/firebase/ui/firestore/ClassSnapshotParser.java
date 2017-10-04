package com.firebase.ui.firestore;

import com.firebase.ui.common.Preconditions;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * An implementation of {@link SnapshotParser} that converts {@link DocumentSnapshot} to
 * a class using {@link DocumentSnapshot#toObject(Class)}.
 */
public class ClassSnapshotParser<T> implements SnapshotParser<T> {

    private final Class<T> mModelClass;

    public ClassSnapshotParser(Class<T> modelClass) {
        mModelClass = Preconditions.checkNotNull(modelClass);
    }

    @Override
    public T parseSnapshot(DocumentSnapshot snapshot) {
        return snapshot.toObject(mModelClass);
    }

}
