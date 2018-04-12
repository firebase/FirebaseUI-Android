package com.firebase.ui.firestore.paging;

import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * All of the data the adapter needs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PagingData {

    private final LiveData<PagedList<DocumentSnapshot>> mSnapshots;
    private final LiveData<LoadingState> mLoadingState;
    private final LiveData<FirestoreDataSource> mDataSource;

    public PagingData(@NonNull LiveData<PagedList<DocumentSnapshot>> snapshots) {
        mSnapshots = snapshots;

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
    }

    public LiveData<PagedList<DocumentSnapshot>> getSnapshots() {
        return mSnapshots;
    }

    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    public LiveData<FirestoreDataSource> getDataSource() { return mDataSource; }

}
