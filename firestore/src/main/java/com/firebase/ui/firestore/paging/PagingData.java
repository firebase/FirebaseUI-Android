package com.firebase.ui.firestore.paging;

import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.RestrictTo;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * All of the data the adapter needs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PagingData {

    private final LiveData<PagedList<DocumentSnapshot>> mSnapshots;
    private final LiveData<LoadingState> mLoadingState;

    public PagingData(FirestoreDataSource.Factory factory,
                      PagedList.Config config) {

        mSnapshots = new LivePagedListBuilder<>(factory, config).build();

        mLoadingState = Transformations.switchMap(factory.getDataSource(),
                new Function<FirestoreDataSource, LiveData<LoadingState>>() {
                    @Override
                    public LiveData<LoadingState> apply(FirestoreDataSource input) {
                        return input.getLoadingState();
                    }
                });
    }

    public LiveData<PagedList<DocumentSnapshot>> getSnapshots() {
        return mSnapshots;
    }

    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

}
