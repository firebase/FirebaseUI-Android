package com.firebase.ui.firestore.paging;

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

import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Paginated RecyclerView Adapter for a Cloud Firestore query.
 *
 * Configured with {@link FirestorePagingOptions}.
 */
public abstract class FirestorePagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends PagedListAdapter<DocumentSnapshot, VH>
        implements LifecycleObserver {

    private static final String TAG = "FirestorePagingAdapter";

    private final SnapshotParser<T> mParser;

    private final LiveData<PagedList<DocumentSnapshot>> mSnapshots;
    private final LiveData<LoadingState> mLoadingState;
    private final LiveData<Exception> mException;
    private final LiveData<FirestoreDataSource> mDataSource;

    /*
        LiveData created via Transformation do not have a value until an Observer is attached.  
        We attach this empty observer so that our getValue() calls return non-null later.
    */
    private final Observer<FirestoreDataSource> mDataSourceObserver = new Observer<FirestoreDataSource>() {
        @Override
        public void onChanged(@Nullable FirestoreDataSource source) {

        }
    };

    //Error observer to determine last occurred Error
    private final Observer<Exception> mErrorObserver = new Observer<Exception>() {
        @Override
        public void onChanged(@Nullable Exception e) {
            onError(e);
        }
    };

    private final Observer<LoadingState> mStateObserver =
            new Observer<LoadingState>() {
                @Override
                public void onChanged(@Nullable LoadingState state) {
                    if (state == null) {
                        return;
                    }

                    onLoadingStateChanged(state);
                }
            };

    private final Observer<PagedList<DocumentSnapshot>> mDataObserver =
            new Observer<PagedList<DocumentSnapshot>>() {
                @Override
                public void onChanged(@Nullable PagedList<DocumentSnapshot> snapshots) {
                    if (snapshots == null) {
                        return;
                    }

                    submitList(snapshots);
                }
            };

    /**
     * Construct a new FirestorePagingAdapter from the given {@link FirestorePagingOptions}.
     */
    public FirestorePagingAdapter(@NonNull FirestorePagingOptions<T> options) {
        super(options.getDiffCallback());

        mSnapshots = options.getData();

        mLoadingState = Transformations.switchMap(mSnapshots,
                new Function<PagedList<DocumentSnapshot>, LiveData<LoadingState>>() {
                    @Override
                    public LiveData<LoadingState> apply(PagedList<DocumentSnapshot> input) {
                        FirestoreDataSource dataSource = (FirestoreDataSource) input.getDataSource();
                        return dataSource.getLoadingState();
                    }
                });

        mDataSource = Transformations.map(mSnapshots,
                new Function<PagedList<DocumentSnapshot>, FirestoreDataSource>() {
                    @Override
                    public FirestoreDataSource apply(PagedList<DocumentSnapshot> input) {
                        return (FirestoreDataSource) input.getDataSource();
                    }
                });

        mException = Transformations.switchMap(mSnapshots,
                new Function<PagedList<DocumentSnapshot>, LiveData<Exception>>() {
                    @Override
                    public LiveData<Exception> apply(PagedList<DocumentSnapshot> input) {
                        FirestoreDataSource dataSource = (FirestoreDataSource) input.getDataSource();
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
    public void retry() {
        FirestoreDataSource source = mDataSource.getValue();
        if (source == null) {
            Log.w(TAG, "Called retry() when FirestoreDataSource is null!");
            return;
        }

        source.retry();
    }

    /**
     * To attempt to refresh the list. It will reload the list from beginning.
     */
    public void refresh(){
        FirestoreDataSource mFirebaseDataSource = mDataSource.getValue();
        if (mFirebaseDataSource == null) {
            Log.w(TAG, "Called refresh() when FirestoreDataSource is null!");
            return;
        }
        mFirebaseDataSource.invalidate();
    }

    /**
     * Start listening to paging / scrolling events and populating adapter data.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mSnapshots.observeForever(mDataObserver);
        mLoadingState.observeForever(mStateObserver);
        mDataSource.observeForever(mDataSourceObserver);
        mException.observeForever(mErrorObserver);
    }

    /**
     * Unsubscribe from paging / scrolling events, no more data will be populated, but the existing
     * data will remain.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeObserver(mDataObserver);
        mLoadingState.removeObserver(mStateObserver);
        mDataSource.removeObserver(mDataSourceObserver);
        mException.removeObserver(mErrorObserver);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot snapshot = getItem(position);
        onBindViewHolder(holder, position, mParser.parseSnapshot(snapshot));
    }

    /**
     * @param model the model object containing the data that should be used to populate the view.
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    protected abstract void onBindViewHolder(@NonNull VH holder, int position, @NonNull T model);

    /**
     * Called whenever the loading state of the adapter changes.
     *
     * When the state is {@link LoadingState#ERROR} the adapter will stop loading any data unless
     * {@link #retry()} is called.
     */
    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        // For overriding
    }

    /**
     * Called whenever the {@link Exception} is caught.
     *
     * When {@link Exception} is caught the adapter will stop loading any data
     */
    protected void onError(@NonNull Exception e) {
        // For overriding
    }
}
