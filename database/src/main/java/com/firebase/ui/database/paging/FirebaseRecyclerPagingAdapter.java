package com.firebase.ui.database.paging;

import android.arch.core.util.Function;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.Transformations;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.annotations.NotNull;

/**
 * Paginated RecyclerView Adapter for a Firebase Realtime Database query.
 *
 * Configured with {@link DatabasePagingOptions}.
 */
public abstract class FirebaseRecyclerPagingAdapter<T, VH extends RecyclerView.ViewHolder> extends PagedListAdapter<DataSnapshot, VH> implements LifecycleObserver{

    private final String TAG = "FirebaseRecyclerPagingAdapter";

    private final SnapshotParser<T> mParser;
    private final LiveData<PagedList<DataSnapshot>> mPagedList;
    private final LiveData<LoadingState> mLoadingState;
    private final LiveData<DatabaseError> mDatabaseError;
    private final LiveData<FirebaseDataSource> mDataSource;

    //State Observer
    private final Observer<LoadingState> mStateObserver = new Observer<LoadingState>() {
        @Override
        public void onChanged(@Nullable LoadingState state) {
            if (state == null) {
                return;
            }

            onLoadingStateChanged(state);
        }
    };

    //Data Observer
    private final Observer<PagedList<DataSnapshot>> mDataObserver = new Observer<PagedList<DataSnapshot>>() {
        @Override
        public void onChanged(@Nullable PagedList<DataSnapshot> snapshots) {
            if (snapshots == null) {
                return;
            }
            submitList(snapshots);
        }
    };

    //DatabaseError Observer
    private final Observer<DatabaseError> mErrorObserver = new Observer<DatabaseError>() {
        @Override
        public void onChanged(@Nullable DatabaseError databaseError) {
            onError(databaseError);
        }
    };

    /**
     * Construct a new FirestorePagingAdapter from the given {@link DatabasePagingOptions}.
     */
    public FirebaseRecyclerPagingAdapter(@NonNull DatabasePagingOptions<T> options){
        super(options.getDiffCallback());

        mPagedList = options.getData();

        //Init Data Source
        mDataSource = Transformations.map(mPagedList,
                new Function<PagedList<DataSnapshot>, FirebaseDataSource>() {
                    @Override
                    public FirebaseDataSource apply(PagedList<DataSnapshot> input) {
                        return (FirebaseDataSource) input.getDataSource();
                    }
                });

        //Init Loading State
        mLoadingState = Transformations.switchMap(mPagedList,
                new Function<PagedList<DataSnapshot>, LiveData<LoadingState>>() {
                    @Override
                    public LiveData<LoadingState> apply(PagedList<DataSnapshot> input) {
                        FirebaseDataSource dataSource = (FirebaseDataSource) input.getDataSource();
                        return dataSource.getLoadingState();
                    }
                });

        //Init Database Error
        mDatabaseError = Transformations.switchMap(mPagedList,
                new Function<PagedList<DataSnapshot>, LiveData<DatabaseError>>() {
                    @Override
                    public LiveData<DatabaseError> apply(PagedList<DataSnapshot> input) {
                        FirebaseDataSource dataSource = (FirebaseDataSource) input.getDataSource();
                        return dataSource.getLastError();
                    }
                });

        mParser = options.getParser();

        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }

    }

    /**
     * Start listening to paging / scrolling events and populating adapter data.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mPagedList.observeForever(mDataObserver);
        mLoadingState.observeForever(mStateObserver);
        mDatabaseError.observeForever(mErrorObserver);
    }

    /**
     * Unsubscribe from paging / scrolling events, no more data will be populated, but the existing
     * data will remain.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mPagedList.removeObserver(mDataObserver);
        mLoadingState.removeObserver(mStateObserver);
        mDatabaseError.removeObserver(mErrorObserver);
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
    protected abstract void onBindViewHolder(@NonNull VH viewHolder, int position, @NotNull T model);

    /**
     * Called whenever the loading state of the adapter changes.
     *
     * When the state is {@link LoadingState#ERROR} the adapter will stop loading any data
     */
    protected abstract void onLoadingStateChanged(@NonNull LoadingState state);

    /**
     * Called whenever the {@link DatabaseError} is caught.
     *
     * When {@link DatabaseError} is caught the adapter will stop loading any data
     */
    protected void onError(@NotNull DatabaseError databaseError){

    }

    @NotNull
    public DatabaseReference getRef(int position){
       return getItem(position).getRef();
    }
}
