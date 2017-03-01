package com.firebase.ui.database;

import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;

/**
 * A convenience implementation of {@link SnapshotParser} that converts a {@link DataSnapshot} to
 * the parametrized class via {@link DataSnapshot#getValue(Class)}.
 *
 * @param <T> the POJO class to create from snapshots.
 */
public class ClassSnapshotParser<T> implements SnapshotParser<T> {
    private Class<T> mClass;

    public ClassSnapshotParser(@NonNull Class<T> clazz) {
        mClass = Preconditions.checkNotNull(clazz);
    }

    @Override
    public T parseSnapshot(DataSnapshot snapshot) {
        return snapshot.getValue(mClass);
    }
}
