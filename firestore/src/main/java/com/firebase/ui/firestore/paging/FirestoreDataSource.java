package com.firebase.ui.firestore.paging;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

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

    private final Query mBaseQuery;
    private final Source mSource;

    public FirestoreDataSource(@NonNull Query baseQuery, @NonNull Source source) {
        mBaseQuery = baseQuery;
        mSource = source;
    }

    @Override
    public void loadInitial(@NonNull final LoadInitialParams<PageKey> params,
                            @NonNull final LoadInitialCallback<PageKey, DocumentSnapshot> callback) {
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

                });

    }

    @NonNull
    private PageKey getNextPageKey(@NonNull QuerySnapshot snapshot) {
        List<DocumentSnapshot> data = snapshot.getDocuments();
        DocumentSnapshot last = getLast(data);

        return new PageKey(last, null);
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
     * Success listener that sets success state and nullifies the retry runnable.
     */
    private abstract class OnLoadSuccessListener implements OnSuccessListener<QuerySnapshot> {

        @Override
        public void onSuccess(QuerySnapshot snapshot) {
            setResult(snapshot);
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
        }
    }
}
