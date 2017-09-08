package com.firebase.ui.firestore;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

/**
 * RecyclerView adapter that listenes to an {@link FirestoreArray} and displays data in real time,
 *
 * @param <T> model class, for parsing {@link DocumentSnapshot}.
 * @param <VH> viewholder class.
 */
public abstract class FirestoreRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements ChangeEventListener, LifecycleObserver {

    private static final String TAG = "FirestoreRecycler";

    private ObservableSnapshotArray<T> mArray;

    /**
     * Create a new RecyclerView adapter to bind data from a Firestore query where each
     * {@link DocumentSnapshot} is converted to the specified model class.
     *
     * See {@link #FirestoreRecyclerAdapter(ObservableSnapshotArray, LifecycleOwner)}.
     *
     * @param query the Firestore query.
     * @param modelClass the model class.
     */
    public FirestoreRecyclerAdapter(Query query, Class<T> modelClass) {
        this(query, modelClass, null);
    }

    /**
     * Create a new RecyclerView adapter bound to a LifecycleOwner.
     *
     * See {@link #FirestoreRecyclerAdapter(Query, Class)}
     */
    public FirestoreRecyclerAdapter(Query query, Class<T> modelClass, LifecycleOwner owner) {
        mArray = new FirestoreArray<>(query, modelClass);
        if (owner != null) {
            owner.getLifecycle().addObserver(this);
        }
    }

    /**
     * Create a new RecyclerView adapter to bind data from a Firestore query where each
     * {@link DocumentSnapshot} is parsed by the specified parser.
     *
     * See {@link #FirestoreRecyclerAdapter(ObservableSnapshotArray, LifecycleOwner)}.
     *
     * @param query the Firestore query.
     * @param parser the snapshot parser.
     */
    public FirestoreRecyclerAdapter(Query query, SnapshotParser<T> parser) {
        this(query, parser, null);
    }

    /**
     * Create a new RecyclerView adapter bound to a LifecycleOwner.
     *
     * See {@link #FirestoreRecyclerAdapter(Query, SnapshotParser)}.
     */
    public FirestoreRecyclerAdapter(Query query, SnapshotParser<T> parser, LifecycleOwner owner) {
        mArray = new FirestoreArray<T>(query, parser);
        if (owner != null) {
            owner.getLifecycle().addObserver(this);
        }
    }

    /**
     * Create a new RecyclerView adapter to bind data from an {@link ObservableSnapshotArray}.
     *
     * @param array the observable array of data from Firestore.
     * @param owner (optional) a LifecycleOwner to observe.
     */
    public FirestoreRecyclerAdapter(ObservableSnapshotArray<T> array,
                                    @Nullable LifecycleOwner owner) {
        mArray = array;
        if (owner != null) {
            owner.getLifecycle().addObserver(this);
        }
    }

    /**
     * Called when data has been added/changed and an item needs to be displayed.
     *
     * @param vh the view to populate.
     * @param i the position in the list of the view being populated.
     * @param model the model object containing the data that should be used to populate the view.
     */
    protected abstract void onBindViewHolder(VH vh, int i, T model);

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mArray.isListening(this)) {
            mArray.addChangeEventListener(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mArray.removeChangeEventListener(this);
        notifyDataSetChanged();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup(LifecycleOwner source) {
        source.getLifecycle().removeObserver(this);
    }

    @Override
    public void onBindViewHolder(VH vh, int i) {
        T model = mArray.getObject(i);
        onBindViewHolder(vh, i, model);
    }

    @Override
    public int getItemCount() {
        return mArray.size();
    }

    @Override
    public void onChildChanged(ChangeEventType type, DocumentSnapshot snapshot,
                               int newIndex, int oldIndex) {

        switch (type) {
            case ADDED:
                notifyItemInserted(newIndex);
                break;
            case REMOVED:
                notifyItemRemoved(oldIndex);
                break;
            case CHANGED:
                notifyItemChanged(newIndex);
                break;
            case MOVED:
                notifyItemMoved(oldIndex, newIndex);
        }
    }

    @Override
    public void onDataChanged() {
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        Log.w(TAG, "onError", e);
    }
}
