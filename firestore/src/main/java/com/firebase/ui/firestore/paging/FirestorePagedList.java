package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.DiffCallback;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Created by samstern on 3/15/18.
 */

public abstract class FirestorePagedList {

    public static LiveData<PagedList<DocumentSnapshot>> getLiveData() {
        DataSource.Factory<PageKey, DocumentSnapshot> factory = new DataSource.Factory<PageKey, DocumentSnapshot>() {

            @Override
            public DataSource<PageKey, DocumentSnapshot> create() {
                // TODO
                Query query = FirebaseFirestore.getInstance().collection("items")
                        .orderBy("value", Query.Direction.ASCENDING);
                return new FirestoreDataSource(query);
            }

        };

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .setPageSize(20)
                .build();

        LiveData<PagedList<DocumentSnapshot>> liveData =
                new LivePagedListBuilder<>(factory, config)
                        .build();

        return liveData;
    }

    public static DiffCallback<DocumentSnapshot> getDiffer() {
        return new DiffCallback<DocumentSnapshot>() {
            @Override
            public boolean areItemsTheSame(@NonNull DocumentSnapshot oldItem,
                                           @NonNull DocumentSnapshot newItem) {

                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull DocumentSnapshot oldItem,
                                              @NonNull DocumentSnapshot newItem) {

                // TODO: Does this even work?
                return oldItem.getData().equals(newItem.getData());
            }
        };
    }

}
