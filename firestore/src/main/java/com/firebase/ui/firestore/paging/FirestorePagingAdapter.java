package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
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
 * TODO(samstern): Document
 */
public abstract class FirestorePagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends PagedListAdapter<DocumentSnapshot, VH> {

    private final SnapshotParser<T> mParser;
    private final LiveData<PagedList<DocumentSnapshot>> mData;

    private final Observer<PagedList<DocumentSnapshot>> mObserver =
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

        // TODO: Lifecycle owner
    }

    // TODO: Unify method names with other adapters
    public void startListening() {
        mData.observeForever(mObserver);
    }

    public void stopListening() {
        mData.removeObserver(mObserver);
    }

    @NonNull
    @Override
    public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot snapshot = getItem(position);
        onBindViewHolder(holder, position, mParser.parseSnapshot(snapshot));
    }

    // TODO: Check that this is the right visibility
    protected abstract void onBindViewHolder(@NonNull VH holder, int position, T model);

    private void onListChanged(@NonNull PagedList<DocumentSnapshot> snapshots) {
        submitList(snapshots);
    }
}
