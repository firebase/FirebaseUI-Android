package com.firebase.ui.firestore.paging;

import android.util.Log;

import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.Transformations;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Paginated RecyclerView Adapter for a Cloud Firestore query.
 *
 * Configured with {@link FirestorePagingOptions}.
 */
public abstract class FirestorePagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends PagedListAdapter<DocumentSnapshot, VH>
        implements LifecycleObserver {

    private static final String TAG = "FirestorePagingAdapter";
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
    private FirestorePagingOptions<T> mOptions;
    private SnapshotParser<T> mParser;
    private LiveData<PagedList<DocumentSnapshot>> mSnapshots;
    private LiveData<LoadingState> mLoadingState;
    private LiveData<Exception> mException;
    private LiveData<FirestoreDataSource> mDataSource;

    /**
     * Construct a new FirestorePagingAdapter from the given {@link FirestorePagingOptions}.
     */
    public FirestorePagingAdapter(@NonNull FirestorePagingOptions<T> options) {
        super(options.getDiffCallback());

        mOptions = options;

        init();
    }

    /**
     * Initializes Snapshots and LiveData
     */
    private void init() {
        mSnapshots = mOptions.getData();

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

        mParser = mOptions.getParser();

        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().addObserver(this);
        }
    }

    /**
     * If {@link #onLoadingStateChanged(LoadingState)} indicates error state, call this method to
     * attempt to retry the most recent failure.
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
    public void refresh() {
        FirestoreDataSource mFirebaseDataSource = mDataSource.getValue();
        if (mFirebaseDataSource == null) {
            Log.w(TAG, "Called refresh() when FirestoreDataSource is null!");
            return;
        }
        mFirebaseDataSource.invalidate();
    }

    /**
     * Re-initialize the Adapter with a new set of options. Can be used to change the query without
     * re-constructing the entire adapter.
     */
    public void updateOptions(@NonNull FirestorePagingOptions<T> options) {
        mOptions = options;

        // Tear down old options
        boolean hasObservers = mSnapshots.hasObservers();
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
     * <p>
     * When the state is {@link LoadingState#ERROR} the adapter will stop loading any data unless
     * {@link #retry()} is called.
     */
    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        // For overriding
    }

    /**
     * Called whenever the {@link Exception} is caught.
     * <p>
     * When {@link Exception} is caught the adapter will stop loading any data
     */
    protected void onError(@NonNull Exception e) {
        Log.w(TAG, "onError", e);
    }
}
