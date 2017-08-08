package com.firebase.ui.common;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Exposes a collection of {@link S} items in a database as a {@link List} of {@link T} objects.
 * To observe the list attach a {@link L} listener.
 *
 * @param <S> the snapshot class.
 * @param <E> the error type raised for the listener.
 * @param <L> the listener class.
 * @param <T> the model object class.
 */
public abstract class BaseObservableSnapshotArray<S, E, L extends BaseChangeEventListener<S, E>, T>
        extends AbstractList<S> {

    private final List<L> mListeners = new CopyOnWriteArrayList<>();
    private BaseSnapshotParser<S, T> mParser;

    /**
     * True if there has been a "data changed" event since the array was created, false otherwise.
     */
    private boolean mHasDataChanged = false;

    /**
     * Default constructor. Must set the {@link BaseSnapshotParser} before the first operation
     * or an exception will be thrown.
     */
    public BaseObservableSnapshotArray() {}

    /**
     * Create an BaseObservableSnapshotArray with a custom {@link BaseSnapshotParser}.
     *
     * @param parser the {@link BaseSnapshotParser} to use
     */
    public BaseObservableSnapshotArray(@NonNull BaseSnapshotParser<S, T> parser) {
        mParser = Preconditions.checkNotNull(parser);
    }

    /**
     * Called when the {@link BaseObservableSnapshotArray} is active and should start listening to the
     * Firebase database.
     */
    @CallSuper
    protected void onCreate() {}

    /**
     * Called when a new listener has been added to the array. This is a good time to pass initial
     * state and fire backlogged events
     * @param listener the added listener.
     */
    @CallSuper
    protected void onListenerAdded(L listener) {
        for (int i = 0; i < size(); i++) {
            listener.onChildChanged(ChangeEventType.ADDED, get(i), i, -1);
        }

        if (mHasDataChanged) {
            listener.onDataChanged();
        }
    };

    /**
     * Called when the {@link BaseObservableSnapshotArray} is inactive and should stop listening to the
     * Firebase database.
     * <p>
     * All data should also be cleared here.
     */
    @CallSuper
    protected void onDestroy() {
        mHasDataChanged = false;
    }

    /**
     * Attach a {@link BaseChangeEventListener} to this array. The listener will receive one {@link
     * ChangeEventType#ADDED} event for each item that already exists in the array at
     * the time of attachment, and then receive all future child events.
     */
    @CallSuper
    public L addChangeEventListener(@NonNull L listener) {
        Preconditions.checkNotNull(listener);
        boolean wasListening = isListening();

        mListeners.add(listener);
        onListenerAdded(listener);

        if (!wasListening) {
            onCreate();
        }

        return listener;
    }

    /**
     * Remove a listener from the array. If no listeners remain, {@link #onDestroy()}
     * will be called.
     */
    @CallSuper
    public void removeChangeEventListener(@NonNull L listener) {
        Preconditions.checkNotNull(listener);
        mListeners.remove(listener);

        if (!isListening()) {
            onDestroy();
        }
    }

    /**
     * Remove all listeners from the array.
     */
    @CallSuper
    public void removeAllListeners() {
        for (L listener : mListeners) {
            removeChangeEventListener(listener);
        }
    }

    /**
     * Get all active listeners.
     */
    public List<L> getListeners() {
        return mListeners;
    }

    /**
     * @return true if the array is listening for change events from the Firebase
     * database, false otherwise
     */
    public final boolean isListening() {
        return !mListeners.isEmpty();
    }

    /**
     * @return true if the provided listener is listening for changes
     */
    public final boolean isListening(L listener) {
        return mListeners.contains(listener);
    }

    /**
     * Get the Snapshot at a given position converted to an object of the parametrized
     * type. This uses the {@link BaseSnapshotParser} passed to the constructor. If the parser was not
     * initialized this will throw an unchecked exception.
     */
    public T getObject(int index) {
        if (mParser == null) {
            throw new IllegalStateException("getObject() called before snapshot parser set.");
        }

        return mParser.parseSnapshot(get(index));
    }

    protected void notifyListenersOnChildChanged(ChangeEventType type,
                                                 S snapshot,
                                                 int newIndex,
                                                 int oldIndex) {
        for (L listener : getListeners()) {
            listener.onChildChanged(type, snapshot, newIndex, oldIndex);
        }
    }

    protected void notifyListenersOnDataChanged() {
        mHasDataChanged = true;

        for (L listener : getListeners()) {
            listener.onDataChanged();
        }
    }

    protected void notifyListenersOnError(E e) {
        for (L listener : getListeners()) {
            listener.onError(e);
        }
    }

    protected BaseSnapshotParser<S, T> getSnapshotParser() {
        return mParser;
    }

    protected void setSnapshotParser(BaseSnapshotParser<S, T> parser) {
        mParser = parser;
    }
}
