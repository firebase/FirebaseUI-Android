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

        // Set initial loading state
        mLoadingState.postValue(LoadingState.LOADING_INITIAL);

        mBaseQuery.limit(params.requestedLoadSize)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        PageKey nextPage = getNextPageKey(snapshot);
                        callback.onResult(snapshot.getDocuments(), null, nextPage);

                        mLoadingState.postValue(LoadingState.LOADED);
                    }
                })
                .addOnFailureListener(new OnLoadFailureListener());

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

        // Set loading state
        mLoadingState.postValue(LoadingState.LOADING_MORE);

        key.getPageQuery(mBaseQuery, params.requestedLoadSize)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshot) {
                        PageKey nextPage = getNextPageKey(snapshot);
                        callback.onResult(snapshot.getDocuments(), nextPage);

                        mLoadingState.postValue(LoadingState.LOADED);
                    }
                })
                .addOnFailureListener(new OnLoadFailureListener());

    }

    private PageKey getNextPageKey(@NonNull QuerySnapshot snapshot) {
        List<DocumentSnapshot> data = snapshot.getDocuments();
        DocumentSnapshot last = getLast(data);

        return new PageKey(last, null);
    }

    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    @Nullable
    private DocumentSnapshot getLast(@NonNull List<DocumentSnapshot> data) {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1);
        }
    }

    /**
     * Error listener that just logs and sets the error state.
     */
    private class OnLoadFailureListener implements OnFailureListener {

        @Override
        public void onFailure(@NonNull Exception e) {
            Log.w(TAG, "load:onFailure", e);

            // On error we do NOT post any value to the PagedList, we just tell
            // the developer that we are now in the error state.
            mLoadingState.postValue(LoadingState.ERROR);
        }
    }
}
