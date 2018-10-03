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
import com.google.firebase.firestore.Source;

import java.util.List;

/**
 * Data source to power a {@link FirestorePagingAdapter}.
 *
 * Note: although loadInitial, loadBefore, and loadAfter are not called on the main thread by the
 *       paging library, we treat them as if they were so that we can facilitate retry without
 *       managing our own thread pool or requiring the user to pass us an executor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirestoreDataSource extends PageKeyedDataSource<PageKey, DocumentSnapshot> {

    private static final String TAG = "FirestoreDataSource";

    public static class Factory extends DataSource.Factory<PageKey, DocumentSnapshot> {

        private final Query mQuery;
        private final Source mSource;

        public Factory(@NonNull Query query, @NonNull Source source) {
            mQuery = query;
            mSource = source;
        }

        @Override
        @NonNull
        public DataSource<PageKey, DocumentSnapshot> create() {
            return new FirestoreDataSource(mQuery, mSource);
        }
    }

    private final MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();

    private final Query mBaseQuery;
    private final Source mSource;

    private Runnable mRetryRunnable;

    public FirestoreDataSource(@NonNull Query baseQuery, @NonNull Source source) {
        mBaseQuery = baseQuery;
        mSource = source;
    }

    @Override
    public void loadInitial(@NonNull final LoadInitialParams<PageKey> params,
                            @NonNull final LoadInitialCallback<PageKey, DocumentSnapshot> callback) {

        // Set initial loading state
        mLoadingState.postValue(LoadingState.LOADING_INITIAL);

        mBaseQuery.limit(params.requestedLoadSize)
                .get(mSource)
                .addOnSuccessListener(new OnLoadSuccessListener() {
                    @Override
                    protected void setResult(@NonNull QuerySnapshot snapshot) {
                        PageKey nextPage = getNextPageKey(snapshot);
                        callback.onResult(snapshot.getDocuments(), null, nextPage);
                    }
                })
                .addOnFailureListener(new OnLoadFailureListener() {
                    @Override
                    protected Runnable getRetryRunnable() {
                        return getRetryLoadInitial(params, callback);
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
    public void loadAfter(@NonNull final LoadParams<PageKey> params,
                          @NonNull final LoadCallback<PageKey, DocumentSnapshot> callback) {
        final PageKey key = params.key;

        // Set loading state
        mLoadingState.postValue(LoadingState.LOADING_MORE);

        key.getPageQuery(mBaseQuery, params.requestedLoadSize)
                .get(mSource)
                .addOnSuccessListener(new OnLoadSuccessListener() {
                    @Override
                    protected void setResult(@NonNull QuerySnapshot snapshot) {
                        PageKey nextPage = getNextPageKey(snapshot);
                        callback.onResult(snapshot.getDocuments(), nextPage);
                    }
                })
                .addOnFailureListener(new OnLoadFailureListener() {
                    @Override
                    protected Runnable getRetryRunnable() {
                        return getRetryLoadAfter(params, callback);
                    }
                });

    }

    @NonNull
    private PageKey getNextPageKey(@NonNull QuerySnapshot snapshot) {
        List<DocumentSnapshot> data = snapshot.getDocuments();
        DocumentSnapshot last = getLast(data);

        return new PageKey(last, null);
    }

    @NonNull
    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    public void retry() {
        LoadingState currentState = mLoadingState.getValue();
        if (currentState != LoadingState.ERROR) {
            Log.w(TAG, "retry() not valid when in state: " + currentState);
            return;
        }

        if (mRetryRunnable == null) {
            Log.w(TAG, "retry() called with no eligible retry runnable.");
            return;
        }

        mRetryRunnable.run();
    }

    @Nullable
    private DocumentSnapshot getLast(@NonNull List<DocumentSnapshot> data) {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1);
        }
    }

    @NonNull
    private Runnable getRetryLoadAfter(@NonNull final LoadParams<PageKey> params,
                                       @NonNull final LoadCallback<PageKey, DocumentSnapshot> callback) {
        return new Runnable() {
            @Override
            public void run() {
                loadAfter(params, callback);
            }
        };
    }

    @NonNull
    private Runnable getRetryLoadInitial(@NonNull final LoadInitialParams<PageKey> params,
                                         @NonNull final LoadInitialCallback<PageKey, DocumentSnapshot> callback) {
        return new Runnable() {
            @Override
            public void run() {
                loadInitial(params, callback);
            }
        };
    }

    /**
     * Success listener that sets success state and nullifies the retry runnable.
     */
    private abstract class OnLoadSuccessListener implements OnSuccessListener<QuerySnapshot> {

        @Override
        public void onSuccess(QuerySnapshot snapshot) {
            setResult(snapshot);
            mLoadingState.postValue(LoadingState.LOADED);

            // Post the 'FINISHED' state when no more pages will be loaded. The data source
            // callbacks interpret an empty result list as a signal to cancel any future loads.
            if (snapshot.getDocuments().isEmpty()) {
                mLoadingState.postValue(LoadingState.FINISHED);
            }

            mRetryRunnable = null;
        }

        protected abstract void setResult(@NonNull QuerySnapshot snapshot);
    }

    /**
     * Error listener that logs, sets the error state, and sets up retry.
     */
    private abstract class OnLoadFailureListener implements OnFailureListener {

        @Override
        public void onFailure(@NonNull Exception e) {
            Log.w(TAG, "load:onFailure", e);

            // On error we do NOT post any value to the PagedList, we just tell
            // the developer that we are now in the error state.
            mLoadingState.postValue(LoadingState.ERROR);

            // Set the retry action
            mRetryRunnable = getRetryRunnable();
        }

        protected abstract Runnable getRetryRunnable();
    }
}
