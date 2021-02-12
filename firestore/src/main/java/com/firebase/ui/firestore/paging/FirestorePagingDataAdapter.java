package com.firebase.ui.firestore.paging;

import android.util.Log;

import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.paging.PagingData;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Paginated RecyclerView Adapter for a Cloud Firestore query.
 *
 * Configured with {@link FirestorePagingOptions}.
 */
public abstract class FirestorePagingDataAdapter<T, VH extends RecyclerView.ViewHolder>
        extends PagingDataAdapter<DocumentSnapshot, VH>
        implements LifecycleObserver {

    private static final String TAG = "FirestorePaging3Adapter";
    private final Observer<PagingData<DocumentSnapshot>> mDataObserver =
            new Observer<PagingData<DocumentSnapshot>>() {
                @Override
                public void onChanged(@Nullable PagingData<DocumentSnapshot> snapshots) {
                    if (snapshots == null) {
                        return;
                    }

                    submitData(mOptions.getOwner().getLifecycle(), snapshots);
                }
            };
    private FirestorePagingOptions<T> mOptions;
    private SnapshotParser<T> mParser;
    private LiveData<PagingData<DocumentSnapshot>> mSnapshots;

    /**
     * Construct a new FirestorePagingAdapter from the given {@link FirestorePagingOptions}.
     */
    public FirestorePagingDataAdapter(@NonNull FirestorePagingOptions<T> options) {
        super(options.getDiffCallback());

        mOptions = options;

        init();
    }

    /**
     * Initializes Snapshots and LiveData
     */
    private void init() {
        mSnapshots = mOptions.getPagingData();

        mParser = mOptions.getParser();

        if (mOptions.getOwner() != null) {
            mOptions.getOwner().getLifecycle().addObserver(this);
        }

        addLoadStateListener(new Function1<CombinedLoadStates, Unit>() {
            @Override
            public Unit invoke(CombinedLoadStates states) {
                LoadState refresh = states.getRefresh();
                LoadState append = states.getAppend();

                if (refresh instanceof LoadState.Loading) {
                    onLoadingStateChanged(LoadingState.LOADING_INITIAL);
                    return null;
                }

                if (refresh instanceof LoadState.Error) {
                    LoadState.Error errorLoadState = (LoadState.Error) refresh;
                    onError(new Exception(errorLoadState.getError()));
                }

                if (append instanceof LoadState.NotLoading) {
                    LoadState.NotLoading notLoading = (LoadState.NotLoading) append;
                    if (notLoading.getEndOfPaginationReached()) {
                        onLoadingStateChanged(LoadingState.FINISHED);
                        return null;
                    }
                    if (refresh instanceof LoadState.NotLoading) {
                        onLoadingStateChanged(LoadingState.LOADED);
                        return null;
                    }
                }

                if (append instanceof LoadState.Loading) {
                    onLoadingStateChanged(LoadingState.LOADING_MORE);
                    return null;
                }

                if (append instanceof LoadState.Error) {
                    LoadState.Error errorLoadState = (LoadState.Error) append;
                    onError(new Exception(errorLoadState.getError()));
                }
                return null;
            }
        });
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
    }

    /**
     * Unsubscribe from paging / scrolling events, no more data will be populated, but the existing
     * data will remain.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeObserver(mDataObserver);
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
