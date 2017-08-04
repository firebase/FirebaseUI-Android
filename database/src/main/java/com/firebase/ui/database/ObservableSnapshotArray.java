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

    private boolean mHasDataChanged = false;

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

        mListeners.add(listener);
        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventListener.EventType.ADDED, get(i), i, -1);
        }

        if (mHasDataChanged) {
            listener.onDataChanged();
        }

        if (!wasListening) { onCreate(); }

        return listener;
    }

    /**
     * Called when the {@link ObservableSnapshotArray} is active and should start listening to the
     * Firebase database.
     */
    @CallSuper
    protected void onCreate() {}

    /**
     * Detach a {@link com.google.firebase.database.ChildEventListener} from this array.
     */
    @CallSuper
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        super.removeChangeEventListener(listener);

        if (!isListening()) { onDestroy(); }
    }

    /**
     * Called when the {@link ObservableSnapshotArray} is inactive and should stop listening to the
     * Firebase database.
     * <p>
     * All data should also be cleared here.
     */
    @CallSuper
    protected void onDestroy() {
        mHasDataChanged = false;
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
        mHasDataChanged = true;
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
