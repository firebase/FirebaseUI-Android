package com.firebase.ui.firestore;

import android.util.Log;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView adapter that listens to a {@link FirestoreArray} and displays its data in real
 * time.
 *
 * @param <T>  model class, for parsing {@link DocumentSnapshot}s.
 * @param <VH> {@link RecyclerView.ViewHolder} class.
 */
public abstract class FirestoreRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements ChangeEventListener, LifecycleObserver {

    private static final String TAG = "FirestoreRecycler";

    private FirestoreRecyclerOptions<T> mOptions;
    private ObservableSnapshotArray<T> mSnapshots;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     */
    public FirestoreRecyclerAdapter(@NonNull FirestoreRecyclerOptions<T> options) {
        mOptions = options;
        mSnapshots = options.getSnapshots();

        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().addObserver(this);
        }
    }

    /**
     * Start listening for database changes and populate the adapter.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    /**
     * Stop listening for database changes and clear all items in the adapter.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeChangeEventListener(this);
        notifyDataSetChanged();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup(LifecycleOwner source) {
        source.getLifecycle().removeObserver(this);
    }

    /**
     * Returns the backing {@link ObservableSnapshotArray} used to populate this adapter.
     *
     * @return the backing snapshot array
     */
    @NonNull
    public ObservableSnapshotArray<T> getSnapshots() {
        return mSnapshots;
    }

    /**
     * Gets the item at the specified position from the backing snapshot array.
     *
     * @see ObservableSnapshotArray#get(int)
     */
    @NonNull
    public T getItem(int position) {
        return mSnapshots.get(position);
    }

    /**
     * Gets the size of snapshots in adapter.
     *
     * @return the total count of snapshots in adapter.
     * @see ObservableSnapshotArray#size()
     */
    @Override
    public int getItemCount() {
        return mSnapshots.isListening(this) ? mSnapshots.size() : 0;
    }

    /**
     * Re-initialize the Adapter with a new set of options. Can be used to change the query without
     * re-constructing the entire adapter.
     */
    public void updateOptions(@NonNull FirestoreRecyclerOptions<T> options) {
        // Tear down old options
        boolean wasListening = mSnapshots.isListening(this);
        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().removeObserver(this);
        }
        mSnapshots.clear();
        stopListening();

        // Set up new options
        mOptions = options;
        mSnapshots = options.getSnapshots();
        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }
        if (wasListening) {
            startListening();
        }
    }

    @Override
    public void onChildChanged(@NonNull ChangeEventType type,
                               @NonNull DocumentSnapshot snapshot,
                               int newIndex,
                               int oldIndex) {
        switch (type) {
            case ADDED:
                notifyItemInserted(newIndex);
                break;
            case CHANGED:
                notifyItemChanged(newIndex);
                break;
            case REMOVED:
                notifyItemRemoved(oldIndex);
                break;
            case MOVED:
                notifyItemMoved(oldIndex, newIndex);
                break;
            default:
                throw new IllegalStateException("Incomplete case statement");
        }
    }

    @Override
    public void onDataChanged() {
    }

    @Override
    public void onError(@NonNull FirebaseFirestoreException e) {
        Log.w(TAG, "onError", e);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        onBindViewHolder(holder, position, getItem(position));
    }

    /**
     * @param model the model object containing the data that should be used to populate the view.
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    protected abstract void onBindViewHolder(@NonNull VH holder, int position, @NonNull T model);
}
