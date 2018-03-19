package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.v7.util.DiffUtil;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

/**
 * TODO(samstern): Document
 */
public abstract class FirestorePagedList {

    private static final DiffUtil.ItemCallback<DocumentSnapshot> DOCUMENT_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DocumentSnapshot>() {
                @Override
                public boolean areItemsTheSame(DocumentSnapshot oldItem, DocumentSnapshot newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(DocumentSnapshot oldItem,
                                                  DocumentSnapshot newItem) {
                    // TODO: Does this even work?
                    return oldItem.getData().equals(newItem.getData());
                }
            };

    public static LiveData<PagedList<DocumentSnapshot>> getLiveData(final Query query) {
        DataSource.Factory<PageKey, DocumentSnapshot> factory =
                new DataSource.Factory<PageKey, DocumentSnapshot>() {
                    @Override
                    public DataSource<PageKey, DocumentSnapshot> create() {
                        return new FirestoreDataSource(query);
                    }
                };

        // TODO: configurable
        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .setPageSize(20)
                .build();

        return new LivePagedListBuilder<>(factory, config).build();
    }

    // TODO: Provide a way to do this with classes
    public static DiffUtil.ItemCallback<DocumentSnapshot> getDiffCallback() {
        return DOCUMENT_DIFF_CALLBACK;
    }

}
