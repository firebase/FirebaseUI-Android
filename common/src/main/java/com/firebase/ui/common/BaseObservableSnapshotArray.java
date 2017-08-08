package com.firebase.ui.common;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Exposes a collection of {@link S} items in a database as a {@link List} of {@link E} objects.
 * To observe the list attach a {@link L} listener.
 *
 * @param <S> the snapshot class.
 * @param <L> the listener class.
 * @param <E> the model object class.
 */
public abstract class BaseObservableSnapshotArray<S, L extends BaseChangeEventListener<S,?>, E>
        extends AbstractList<S> {

    private final List<L> mListeners = new CopyOnWriteArrayList<>();
    private BaseSnapshotParser<S, E> mParser;

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
    public BaseObservableSnapshotArray(@NonNull BaseSnapshotParser<S, E> parser) {
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
    protected void onListenerAdded(L listener) {};

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
     * Attach a listener to this array.
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
    public E getObject(int index) {
        if (mParser == null) {
            throw new IllegalStateException("getObject() called before snapshot parser set.");
        }

        return mParser.parseSnapshot(get(index));
    }

    @Override
    public S get(int index) {
        return getSnapshots().get(index);
    }

    @Override
    public int size() {
        return getSnapshots().size();
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


    // TODO(samstern): Do we need this?
    protected abstract List<S> getSnapshots();

    protected boolean hasDataChanged() {
        return mHasDataChanged;
    }

    protected void setHasDataChanged(boolean hasDataChanged) {
        mHasDataChanged = hasDataChanged;
    }

    protected BaseSnapshotParser<S, E> getSnapshotParser() {
        return mParser;
    }

    protected void setSnapshotParser(BaseSnapshotParser<S, E> parser) {
        mParser = parser;
    }
}
