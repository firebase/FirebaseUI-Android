package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
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
    private final PagingData mData;

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

    public FirestorePagingAdapter(@NonNull FirestorePagingOptions<T> options) {
        super(options.getDiffCallback());

        mParser = options.getParser();
        mData = options.getData();

        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }
    }

    /**
     * If {@link #onLoadingStateChanged(LoadingState)} indicates error state, call this method
     * to attempt to retry the most recent failure.
     */
    public void retry() {
        FirestoreDataSource source = mData.getDataSource().getValue();
        if (source == null) {
            Log.w(TAG, "Called retry() when FirestoreDataSource is null!");
            return;
        }

        source.retry();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mData.getSnapshots().observeForever(mDataObserver);
        mData.getLoadingState().observeForever(mStateObserver);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mData.getSnapshots().removeObserver(mDataObserver);
        mData.getLoadingState().removeObserver(mStateObserver);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot snapshot = getItem(position);
        onBindViewHolder(holder, position, mParser.parseSnapshot(snapshot));
    }

    protected abstract void onBindViewHolder(@NonNull VH holder, int position, T model);

    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        // For overriding
    }
}
