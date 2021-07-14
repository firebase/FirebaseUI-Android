package com.firebase.ui.database.paging;

import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.paging.PagingData;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Paginated RecyclerView Adapter for a Firebase Realtime Database query.
 *
 * Configured with {@link DatabasePagingOptions}.
 */
public abstract class FirebaseRecyclerPagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends PagingDataAdapter<DataSnapshot, VH>
        implements LifecycleObserver {

    private DatabasePagingOptions<T> mOptions;
    private SnapshotParser<T> mParser;
    private LiveData<PagingData<DataSnapshot>> mPagingData;

    //Data Observer
    private final Observer<PagingData<DataSnapshot>> mDataObserver = new Observer<PagingData<DataSnapshot>>() {
        @Override
        public void onChanged(@Nullable PagingData<DataSnapshot> snapshots) {
            if (snapshots == null) {
                return;
            }
            submitData(mOptions.getOwner().getLifecycle(), snapshots);
        }
    };

    /**
     * Construct a new FirestorePagingAdapter from the given {@link DatabasePagingOptions}.
     */
    public FirebaseRecyclerPagingAdapter(@NonNull DatabasePagingOptions<T> options){
        super(options.getDiffCallback());

        mOptions = options;

        init();
    }

    /**
     * Initializes Snapshots and LiveData
     */
    public void init() {
        mPagingData = mOptions.getData();

        mParser = mOptions.getParser();

        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().addObserver(this);
        }
    }

    /**
     * Re-initialize the Adapter with a new set of options. Can be used to change the query without
     * re-constructing the entire adapter.
     */
    public void updateOptions(@NonNull DatabasePagingOptions<T> options) {
        mOptions = options;

        // Tear down old options
        boolean hasObservers = mPagingData.hasObservers();
        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().removeObserver(this);
        }
        stopListening();

        // Reinit Options
        init();

        if (hasObservers) {
            startListening();
        }
    }

    /**
     * Start listening to paging / scrolling events and populating adapter data.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mPagingData.observeForever(mDataObserver);
    }

    /**
     * Unsubscribe from paging / scrolling events, no more data will be populated, but the existing
     * data will remain.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mPagingData.removeObserver(mDataObserver);
    }

    @Override
    public void onBindViewHolder(@NonNull VH viewHolder, int position) {
        DataSnapshot snapshot = getItem(position);
        onBindViewHolder(viewHolder, position, mParser.parseSnapshot(snapshot));
    }

    /**
     * @param model the model object containing the data that should be used to populate the view.
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    protected abstract void onBindViewHolder(@NonNull VH viewHolder, int position, @NonNull T model);

    @NonNull
    public DatabaseReference getRef(int position){
       return getItem(position).getRef();
    }

}
