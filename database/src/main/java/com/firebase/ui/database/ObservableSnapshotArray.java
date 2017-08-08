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
 * @param <E> a POJO class to which the DataSnapshots can be converted.
 */
public abstract class ObservableSnapshotArray<E>
        extends BaseObservableSnapshotArray<DataSnapshot, ChangeEventListener, E> {

    /**
     * Default constructor. Must set the snapshot parser before user.
     */
    public ObservableSnapshotArray() {
        super();
    }

    /**
     * Create an ObservableSnapshotArray where snapshots are parsed as objects of a particular
     * class.
     *
     * @param clazz the class as which DataSnapshots should be parsed.
     * @see ClassSnapshotParser
     */
    public ObservableSnapshotArray(@NonNull Class<E> clazz) {
        super(new ClassSnapshotParser<>(clazz));
    }

    /**
     * Create an ObservableSnapshotArray with a custom {@link SnapshotParser}.
     *
     * @param parser the {@link SnapshotParser} to use
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<E> parser) {
        super(parser);
    }

    protected void notifyListenersOnCancelled(DatabaseError error) {
        for (ChangeEventListener listener : getListeners()) {
            listener.onError(error);
        }
    }
}
