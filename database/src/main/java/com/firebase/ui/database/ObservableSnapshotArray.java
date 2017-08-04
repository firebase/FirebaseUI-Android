package com.firebase.ui.database;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

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
     * ChangeEventListener.EventType#ADDED} event for each item that already exists in the array at
     * the time of attachment, and then receive all future child events.
     */
    @CallSuper
    public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
        super.addChangeEventListener(listener);
        boolean wasListening = isListening();

        // TODO(samstern): Can some of this be moved into common?
        mListeners.add(listener);
        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventListener.EventType.ADDED, get(i), i, -1);
        }

        if (hasDataChanged()) {
            listener.onDataChanged();
        }

        if (!wasListening) { onCreate(); }

        return listener;
    }


    protected final void notifyChangeEventListeners(ChangeEventListener.EventType type,
                                                    DataSnapshot snapshot,
                                                    int index) {
        notifyChangeEventListeners(type, snapshot, index, -1);
    }

    protected final void notifyChangeEventListeners(ChangeEventListener.EventType type,
                                                    DataSnapshot snapshot,
                                                    int index,
                                                    int oldIndex) {
        for (ChangeEventListener listener : mListeners) {
            listener.onChildChanged(type, snapshot, index, oldIndex);
        }
    }

    protected final void notifyListenersOnDataChanged() {
        setHasDataChanged(true);
        for (ChangeEventListener listener : mListeners) {
            listener.onDataChanged();
        }
    }

    protected final void notifyListenersOnCancelled(DatabaseError error) {
        for (ChangeEventListener listener : mListeners) {
            listener.onCancelled(error);
        }
    }
}
