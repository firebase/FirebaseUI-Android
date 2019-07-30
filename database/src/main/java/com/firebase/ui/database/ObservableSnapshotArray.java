package com.firebase.ui.database;

import android.support.annotation.NonNull;

import com.firebase.ui.common.BaseObservableSnapshotArray;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.List;

/**
 * Exposes a collection of items in Firebase as a {@link List} of {@link DataSnapshot}. To observe
 * the list attach a {@link com.google.firebase.database.ChildEventListener}.
 *
 * @param <T> a POJO class to which the DataSnapshots can be converted.
 */
public abstract class ObservableSnapshotArray<T>
        extends BaseObservableSnapshotArray<DataSnapshot, DatabaseError, ChangeEventListener, T> {
    /**
     * Create an ObservableSnapshotArray with a custom {@link SnapshotParser}.
     *
     * @param parser the {@link SnapshotParser} to use
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<T> parser) {
        super(new CachingSnapshotParser<>(parser));
    }
}
