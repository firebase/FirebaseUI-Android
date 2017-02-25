package com.firebase.ui.database;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO(samstern): Document and document all methods
 */
public abstract class ObservableSnapshotArray<T> extends ImmutableList<DataSnapshot> {

    protected final List<ChangeEventListener> mListeners = new CopyOnWriteArrayList<>();
    protected final SnapshotParser<T> mParser;

    public ObservableSnapshotArray() {
        mParser = null;
    }

    public ObservableSnapshotArray(SnapshotParser<T> parser) {
        mParser = parser;
    }

    /**
     * TODO(samstern): Document.
     */
    @CallSuper
    public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
        Preconditions.checkNotNull(listener);

        mListeners.add(listener);
        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventListener.EventType.ADDED, get(i), i, -1);
        }

        return listener;
    }

    /**
     * TODO(samstern): Document.
     */
    @CallSuper
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Removes all {@link ChangeEventListener}s. The list will be empty after this call returns.
     *
     * @see #removeChangeEventListener(ChangeEventListener)
     */
    @CallSuper
    public void removeAllListeners() {
        for (ChangeEventListener listener : mListeners) {
            removeChangeEventListener(listener);
        }
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
        for (ChangeEventListener listener : mListeners) {
            listener.onDataChanged();
        }
    }

    protected final void notifyListenersOnCancelled(DatabaseError error) {
        for (ChangeEventListener listener : mListeners) {
            listener.onCancelled(error);
        }
    }

    /**
     * @return true if {@link FirebaseArray} is listening for change events from the Firebase
     * database, false otherwise
     */
    public final boolean isListening() {
        return !mListeners.isEmpty();
    }

    /**
     * @return true if the provided {@link ChangeEventListener} is listening for changes
     */
    public final boolean isListening(ChangeEventListener listener) {
        return mListeners.contains(listener);
    }

    // TODO(samstern): Document
    public T getObject(int index) {
        Preconditions.checkNotNull(mParser);
        return mParser.parseSnapshot(get(index));
    }

}
