package com.firebase.ui.database;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Exposes a collection of items in Firebase as a {@link List} of {@link DataSnapshot}. To observe
 * the list attach a {@link com.google.firebase.database.ChildEventListener}.
 *
 * @param <E> a POJO class to which the DataSnapshots can be converted.
 */
public abstract class ObservableSnapshotArray<E> extends AbstractList<DataSnapshot> {
    protected final List<ChangeEventListener> mListeners = new CopyOnWriteArrayList<>();
    protected final SnapshotParser<E> mParser;

    private boolean mHasDataChanged = false;

    /**
     * Create an ObservableSnapshotArray where snapshots are parsed as objects of a particular
     * class.
     *
     * @param clazz the class as which DataSnapshots should be parsed.
     * @see ClassSnapshotParser
     */
    public ObservableSnapshotArray(@NonNull Class<E> clazz) {
        this(new ClassSnapshotParser<>(clazz));
    }

    /**
     * Create an ObservableSnapshotArray with a custom {@link SnapshotParser}.
     *
     * @param parser the {@link SnapshotParser} to use
     */
    public ObservableSnapshotArray(@NonNull SnapshotParser<E> parser) {
        mParser = Preconditions.checkNotNull(parser);
    }

    /**
     * Attach a {@link ChangeEventListener} to this array. The listener will receive one {@link
     * ChangeEventListener.EventType#ADDED} event for each item that already exists in the array at
     * the time of attachment, and then receive all future child events.
     */
    @CallSuper
    public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
        Preconditions.checkNotNull(listener);

        mListeners.add(listener);
        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventListener.EventType.ADDED, get(i), i, -1);
        }
        if (mHasDataChanged) {
            listener.onDataChanged();
        }

        return listener;
    }

    /**
     * Detach a {@link com.google.firebase.database.ChildEventListener} from this array.
     */
    @CallSuper
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        mListeners.remove(listener);

        // Reset mHasDataChanged if there are no more listeners
        if (!isListening()) {
            mHasDataChanged = false;
        }
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

    protected abstract List<DataSnapshot> getSnapshots();

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

    /**
     * Get the {@link DataSnapshot} at a given position converted to an object of the parametrized
     * type. This uses the {@link SnapshotParser} passed to the constructor. If the parser was not
     * initialized this will throw an unchecked exception.
     */
    public E getObject(int index) {
        return mParser.parseSnapshot(get(index));
    }

    @Override
    public DataSnapshot get(int index) {
        return getSnapshots().get(index);
    }

    @Override
    public int size() {
        return getSnapshots().size();
    }
}
