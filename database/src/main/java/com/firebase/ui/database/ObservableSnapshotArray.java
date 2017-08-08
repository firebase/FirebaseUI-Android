package com.firebase.ui.database;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.firebase.ui.common.BaseObservableSnapshotArray;
import com.firebase.ui.common.ChangeEventType;
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

    /**
     * Attach a {@link ChangeEventListener} to this array. The listener will receive one {@link
     * ChangeEventType#ADDED} event for each item that already exists in the array at
     * the time of attachment, and then receive all future child events.
     */
    @CallSuper
    public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
        return super.addChangeEventListener(listener);
    }

    @Override
    protected void onListenerAdded(ChangeEventListener listener) {
        super.onListenerAdded(listener);

        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventType.ADDED, get(i), i, -1);
        }

        if (hasDataChanged()) {
            listener.onDataChanged();
        }
    }

    protected void notifyListenersOnCancelled(DatabaseError error) {
        for (ChangeEventListener listener : getListeners()) {
            listener.onError(error);
        }
    }
}
