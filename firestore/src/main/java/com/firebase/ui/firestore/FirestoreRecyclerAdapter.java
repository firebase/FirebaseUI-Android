package com.firebase.ui.firestore;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

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

    private ObservableSnapshotArray<T> mSnapshots;

    /**
     * Create a new RecyclerView adapter to bind data from an {@link ObservableSnapshotArray}.
     *
     * @param snapshots the observable array of data from Firestore.
     * @param owner     (optional) a LifecycleOwner to observe.
     */
    public FirestoreRecyclerAdapter(ObservableSnapshotArray<T> snapshots, LifecycleOwner owner) {
        mSnapshots = snapshots;
        if (owner != null) {
            owner.getLifecycle().addObserver(this);
        }
    }

    /**
     * @see #FirestoreRecyclerAdapter(ObservableSnapshotArray, LifecycleOwner)
     */
    public FirestoreRecyclerAdapter(ObservableSnapshotArray<T> snapshots) {
        this(snapshots, null);
    }

    /**
     * Create a new RecyclerView adapter to bind data from a Firestore query where each {@link
     * DocumentSnapshot} is converted to the specified model class.
     *
     * @param query      the Firestore query.
     * @param modelClass the model class.
     * @see #FirestoreRecyclerAdapter(ObservableSnapshotArray, LifecycleOwner)
     */
    public FirestoreRecyclerAdapter(Query query, Class<T> modelClass, LifecycleOwner owner) {
        this(new FirestoreArray<>(query, modelClass), owner);
    }

    /**
     * @see #FirestoreRecyclerAdapter(Query, Class, LifecycleOwner)
     */
    public FirestoreRecyclerAdapter(Query query, Class<T> modelClass) {
        this(query, modelClass, null);
    }

    /**
     * Create a new RecyclerView adapter to bind data from a Firestore query where each {@link
     * DocumentSnapshot} is parsed by the specified parser.
     *
     * @param query  the Firestore query.
     * @param parser the snapshot parser.
     * @see #FirestoreRecyclerAdapter(ObservableSnapshotArray, LifecycleOwner)
     */
    public FirestoreRecyclerAdapter(Query query, SnapshotParser<T> parser, LifecycleOwner owner) {
        this(new FirestoreArray<>(query, parser), owner);
    }

    /**
     * @see #FirestoreRecyclerAdapter(Query, SnapshotParser, LifecycleOwner)
     */
    public FirestoreRecyclerAdapter(Query query, SnapshotParser<T> parser) {
        this(query, parser, null);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeChangeEventListener(this);
        notifyDataSetChanged();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup(LifecycleOwner source) {
        source.getLifecycle().removeObserver(this);
    }

    public ObservableSnapshotArray<T> getSnapshots() {
        return mSnapshots;
    }

    public T getItem(int position) {
        return mSnapshots.getObject(position);
    }

    @Override
    public int getItemCount() {
        return mSnapshots.size();
    }

    @Override
    public void onChildChanged(ChangeEventType type, DocumentSnapshot snapshot,
                               int newIndex, int oldIndex) {
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
    public void onError(FirebaseFirestoreException e) {
        Log.w(TAG, "onError", e);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        onBindViewHolder(holder, position, getItem(position));
    }

    /**
     * Called when data has been added/changed and an item needs to be displayed.
     *
     * @param holder   the view to populate.
     * @param position the position in the list of the view being populated.
     * @param model    the model object containing the data that should be used to populate the
     *                 view.
     */
    protected abstract void onBindViewHolder(VH holder, int position, T model);
}
