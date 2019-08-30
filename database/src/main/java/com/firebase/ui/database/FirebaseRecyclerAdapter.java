package com.firebase.ui.database;

import android.util.Log;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This class is a generic way of backing a {@link RecyclerView} with a Firebase location. It
 * handles all of the child events at the given Firebase location and marshals received data into
 * the given class type.
 * <p>
 * See the <a href="https://github.com/firebase/FirebaseUI-Android/blob/master/database/README.md">README</a>
 * for an in-depth tutorial on how to set up the FirebaseRecyclerAdapter.
 *
 * @param <T>  The Java class that maps to the type of objects stored in the Firebase location.
 * @param <VH> The {@link RecyclerView.ViewHolder} class that contains the Views in the layout that
 *             is shown for each object.
 */
public abstract class FirebaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements FirebaseAdapter<T> {
    private static final String TAG = "FirebaseRecyclerAdapter";

    private FirebaseRecyclerOptions<T> mOptions;
    private ObservableSnapshotArray<T> mSnapshots;

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     */
    public FirebaseRecyclerAdapter(@NonNull FirebaseRecyclerOptions<T> options) {
        mOptions = options;
        mSnapshots = options.getSnapshots();

        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().addObserver(this);
        }
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeChangeEventListener(this);
        notifyDataSetChanged();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup(LifecycleOwner source) {
        source.getLifecycle().removeObserver(this);
    }

    @Override
    public void onChildChanged(@NonNull ChangeEventType type,
                               @NonNull DataSnapshot snapshot,
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
                notifyItemRemoved(newIndex);
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
    public void onError(@NonNull DatabaseError error) {
        Log.w(TAG, error.toException());
    }

    @NonNull
    @Override
    public ObservableSnapshotArray<T> getSnapshots() {
        return mSnapshots;
    }

    @NonNull
    @Override
    public T getItem(int position) {
        return mSnapshots.get(position);
    }

    @NonNull
    @Override
    public DatabaseReference getRef(int position) {
        return mSnapshots.getSnapshot(position).getRef();
    }

    @Override
    public int getItemCount() {
        return mSnapshots.isListening(this) ? mSnapshots.size() : 0;
    }

    /**
     * Re-initialize the Adapter with a new set of options. Can be used to change the query
     * without re-constructing the entire adapter.
     */
    public void updateOptions(@NonNull FirebaseRecyclerOptions<T> options) {
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
    public void onBindViewHolder(@NonNull VH holder, int position) {
        onBindViewHolder(holder, position, getItem(position));
    }

    /**
     * @param model the model object containing the data that should be used to populate the view.
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    protected abstract void onBindViewHolder(@NonNull VH holder, int position, @NonNull T model);
}
