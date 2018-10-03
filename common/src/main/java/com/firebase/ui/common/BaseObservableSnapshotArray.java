package com.firebase.ui.common;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Exposes a collection of {@link S} items in a database as a {@link List} of {@link T} objects. To
 * observe the list, attach an {@link L} listener.
 *
 * @param <S> the snapshot class.
 * @param <E> the error type raised for the listener.
 * @param <L> the listener class.
 * @param <T> the model object class.
 */
public abstract class BaseObservableSnapshotArray<S, E, L extends BaseChangeEventListener<S, E>, T>
        extends AbstractList<T> {

    private final List<L> mListeners = new CopyOnWriteArrayList<>();
    private final BaseCachingSnapshotParser<S, T> mCachingParser;

    /**
     * True if there has been a "data changed" event since the array was created or last reset,
     * false otherwise.
     */
    private boolean mHasDataChanged = false;

    /**
     * Create an BaseObservableSnapshotArray with a custom {@link BaseSnapshotParser}.
     *
     * @param parser the {@link BaseSnapshotParser} to use
     */
    public BaseObservableSnapshotArray(@NonNull BaseCachingSnapshotParser<S, T> parser) {
        mCachingParser = Preconditions.checkNotNull(parser);
    }

    /**
     * Get the list of snapshots mirroring the server's data. Must be mutable and use a single
     * instance over the lifetime of this class's active lifecycle.
     *
     * @return the local copy of the server's snapshots
     */
    @NonNull
    protected abstract List<S> getSnapshots();

    @Override
    @NonNull
    public T get(int index) {
        return mCachingParser.parseSnapshot(getSnapshot(index));
    }

    @Override
    public int size() {
        return getSnapshots().size();
    }

    /**
     * Returns the snapshot at the specified position in this list.
     *
     * @param index index of the snapshot to return
     * @return the snapshot at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index
     *                                   &gt;= size()</tt>)
     */
    @NonNull
    public S getSnapshot(int index) {
        return getSnapshots().get(index);
    }

    /**
     * Attach a {@link BaseChangeEventListener} to this array. The listener will receive one {@link
     * ChangeEventType#ADDED} event for each item that already exists in the array at the time of
     * attachment, a {@link BaseChangeEventListener#onDataChanged()} event if one has occurred, and
     * then receive all future child events.
     * <p>
     * If this is the first listener, {@link #onCreate()} will be called.
     */
    @CallSuper
    @NonNull
    public L addChangeEventListener(@NonNull L listener) {
        Preconditions.checkNotNull(listener);
        boolean wasListening = isListening();

        mListeners.add(listener);

        // Catch up new listener to existing state
        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventType.ADDED, getSnapshot(i), i, -1);
        }
        if (mHasDataChanged) {
            listener.onDataChanged();
        }

        if (!wasListening) { onCreate(); }

        return listener;
    }

    /**
     * Remove a listener from the array.
     * <p>
     * If no listeners remain, {@link #onDestroy()} will be called.
     */
    @CallSuper
    public void removeChangeEventListener(@NonNull L listener) {
        Preconditions.checkNotNull(listener);

        boolean wasListening = isListening();

        mListeners.remove(listener);

        if (!isListening() && wasListening) { onDestroy(); }
    }

    /**
     * Remove all listeners from the array and reset its state.
     */
    @CallSuper
    public void removeAllListeners() {
        for (L listener : mListeners) {
            removeChangeEventListener(listener);
        }
    }

    /**
     * Called when the {@link BaseObservableSnapshotArray} is active and should start listening to
     * the Firebase database.
     */
    @CallSuper
    protected void onCreate() {}

    /**
     * Called when the {@link BaseObservableSnapshotArray} is inactive and should stop listening to
     * the Firebase database.
     * <p>
     * All data and saved state should also be cleared here.
     */
    @CallSuper
    protected void onDestroy() {
        mHasDataChanged = false;
        getSnapshots().clear();
        mCachingParser.clear();
    }

    /**
     * @return true if the array is listening for change events from the Firebase database, false
     * otherwise
     */
    public boolean isListening() {
        return !mListeners.isEmpty();
    }

    /**
     * @return true if the provided listener is listening for changes
     */
    public boolean isListening(@NonNull L listener) {
        return mListeners.contains(listener);
    }

    protected final void notifyOnChildChanged(@NonNull ChangeEventType type,
                                              @NonNull S snapshot,
                                              int newIndex,
                                              int oldIndex) {
        if (type == ChangeEventType.CHANGED || type == ChangeEventType.REMOVED) {
            mCachingParser.invalidate(snapshot);
        }

        for (L listener : mListeners) {
            listener.onChildChanged(type, snapshot, newIndex, oldIndex);
        }
    }

    protected final void notifyOnDataChanged() {
        mHasDataChanged = true;

        for (L listener : mListeners) {
            listener.onDataChanged();
        }
    }

    protected final void notifyOnError(@NonNull E e) {
        for (L listener : mListeners) {
            listener.onError(e);
        }
    }
}
