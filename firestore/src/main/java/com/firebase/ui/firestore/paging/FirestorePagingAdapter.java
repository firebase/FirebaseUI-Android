package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

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

                    onListChanged(snapshots);
                }
            };

    public static class DiffCallback<T> extends DiffUtil.ItemCallback<DocumentSnapshot> {

        private final SnapshotParser<T> mParser;

        public DiffCallback(SnapshotParser<T> parser) {
            mParser = parser;
        }

        @Override
        public boolean areItemsTheSame(DocumentSnapshot oldItem, DocumentSnapshot newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(DocumentSnapshot oldItem, DocumentSnapshot newItem) {
            T oldModel = mParser.parseSnapshot(oldItem);
            T newModel = mParser.parseSnapshot(newItem);

            return oldModel.equals(newModel);
        }
    }

    public FirestorePagingAdapter(@NonNull FirestorePagingOptions<T> options) {
        super(new DiffCallback<>(options.getParser()));

        mParser = options.getParser();
        mData = options.getData();

        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }
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

    @NonNull
    @Override
    public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot snapshot = getItem(position);
        onBindViewHolder(holder, position, mParser.parseSnapshot(snapshot));
    }

    protected abstract void onBindViewHolder(@NonNull VH holder, int position, T model);

    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        // For overriding
    }

    private void onListChanged(@NonNull PagedList<DocumentSnapshot> snapshots) {
        submitList(snapshots);
    }
}
