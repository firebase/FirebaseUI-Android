package com.firebase.ui.firestore.paging;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.DataSource;
import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.List;

/**
 * Data source to power a {@link FirestorePagingAdapter}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirestoreDataSource extends PageKeyedDataSource<PageKey, DocumentSnapshot> {

    private static final String TAG = "FirestoreDataSource";

    public static class Factory extends DataSource.Factory<PageKey, DocumentSnapshot> {

        private final Query mQuery;

        public Factory(Query query) {
            mQuery = query;
        }

        @Override
        public DataSource<PageKey, DocumentSnapshot> create() {
            return new FirestoreDataSource(mQuery);
        }
    }

    private final MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();

    private final Query mBaseQuery;

    public FirestoreDataSource(Query baseQuery) {
        mBaseQuery = baseQuery;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<PageKey> params,
                            @NonNull final LoadInitialCallback<PageKey, DocumentSnapshot> callback) {
        Log.d(TAG, "loadInitial: " + params.requestedLoadSize);

        // Set initial loading state
        mLoadingState.postValue(LoadingState.LOADING_INITIAL);

        mBaseQuery.limit(params.requestedLoadSize)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        List<DocumentSnapshot> data = snapshots.getDocuments();
                        DocumentSnapshot last = getLast(data);

                        PageKey nextPage = new PageKey(last, null);
                        callback.onResult(data, null, nextPage);

                        mLoadingState.postValue(LoadingState.LOADED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "loadInitial:failure", e);

                        // On error, return an empty page with the next page key being basically
                        // equal to the initial query.
                        PageKey nextPage = new PageKey(null, null);
                        callback.onResult(Collections.<DocumentSnapshot>emptyList(),
                                null, nextPage);

                        mLoadingState.postValue(LoadingState.ERROR);
                    }
                });

    }

    @Override
    public void loadBefore(@NonNull LoadParams<PageKey> params,
                           @NonNull LoadCallback<PageKey, DocumentSnapshot> callback) {
        // Ignored for now, since we only ever append to the initial load.
        // Future work:
        //  * Could we dynamically unload past pages?
        //  * Could we ask the developer for both a forward and reverse base query
        //    so that we can load backwards easily?
    }

    @Override
    public void loadAfter(@NonNull LoadParams<PageKey> params,
                          @NonNull final LoadCallback<PageKey, DocumentSnapshot> callback) {
        final PageKey key = params.key;
        Log.d(TAG, "loadAfter: " + key + ", " + params.requestedLoadSize);

        // Set loading state
        mLoadingState.postValue(LoadingState.LOADING_MORE);

        key.getPageQuery(mBaseQuery, params.requestedLoadSize)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        List<DocumentSnapshot> data = snapshots.getDocuments();
                        DocumentSnapshot last = getLast(data);

                        PageKey nextPage = new PageKey(last, null);
                        callback.onResult(data, nextPage);

                        mLoadingState.postValue(LoadingState.LOADED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "loadAfter:failure", e);

                        // On error, return an empty page with the next page key being basically
                        // equal to the initial query.
                        callback.onResult(Collections.<DocumentSnapshot>emptyList(), key);

                        mLoadingState.postValue(LoadingState.ERROR);
                    }
                });

    }

    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    @Nullable
    private DocumentSnapshot getLast(List<DocumentSnapshot> data) {
        if (data == null || data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1);
        }
    }
}
