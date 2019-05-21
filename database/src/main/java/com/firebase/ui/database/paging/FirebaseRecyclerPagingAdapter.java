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
import android.util.Log;

import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

/**
 * Paginated RecyclerView Adapter for a Firebase Realtime Database query.
 *
 * Configured with {@link DatabasePagingOptions}.
 */
public abstract class FirebaseRecyclerPagingAdapter<T, VH extends RecyclerView.ViewHolder> extends PagedListAdapter<DataSnapshot, VH> implements LifecycleObserver{

    private final String TAG = "FirebasePagingAdapter";

    private final SnapshotParser<T> mParser;
    private final LiveData<PagedList<DataSnapshot>> mPagedList;
    private final LiveData<LoadingState> mLoadingState;
    private final LiveData<DatabaseError> mDatabaseError;
    private final LiveData<FirebaseDataSource> mDataSource;


    /*
        LiveData created via Transformation do not have a value until an Observer is attached.  
        We attach this empty observer so that our getValue() calls return non-null later.
    */
    private final Observer<FirebaseDataSource> mDataSourceObserver = new Observer<FirebaseDataSource>() {
        @Override
        public void onChanged(@Nullable FirebaseDataSource source) {

        }
    };

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
     * If {@link #onLoadingStateChanged(LoadingState)} indicates error state, call this method
     * to attempt to retry the most recent failure.
     */
    public void retry(){
        FirebaseDataSource mFirebaseDataSource = mDataSource.getValue();
        if (mFirebaseDataSource == null) {
            Log.w(TAG, "Called retry() when FirebaseDataSource is null!");
            return;
        }

        mFirebaseDataSource.retry();
    }

    /**
     * To attempt to refresh the list. It will reload the list from beginning.
     */
    public void refresh(){
        FirebaseDataSource mFirebaseDataSource = mDataSource.getValue();
        if (mFirebaseDataSource == null) {
            Log.w(TAG, "Called refresh() when FirebaseDataSource is null!");
            return;
        }
        mFirebaseDataSource.invalidate();
    }

    /**
     * Start listening to paging / scrolling events and populating adapter data.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mPagedList.observeForever(mDataObserver);
        mLoadingState.observeForever(mStateObserver);
        mDatabaseError.observeForever(mErrorObserver);
        mDataSource.observeForever(mDataSourceObserver);
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
        mDataSource.removeObserver(mDataSourceObserver);
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
    protected void onError(@NonNull DatabaseError databaseError){

    }

    @NonNull
    public DatabaseReference getRef(int position){
       return getItem(position).getRef();
    }

}
