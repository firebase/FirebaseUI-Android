package com.firebase.ui.firestore.paging;

import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

/**
 * Created by samstern on 3/15/18.
 */

public class FirestoreDataSource extends PageKeyedDataSource<PageKey, DocumentSnapshot> {

    private static final String TAG = "FirestoreDataSource";

    private final Query mBaseQuery;

    public FirestoreDataSource(Query baseQuery) {
        mBaseQuery = baseQuery;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<PageKey> params,
                            @NonNull final LoadInitialCallback<PageKey, DocumentSnapshot> callback) {

        Log.d(TAG, "loadInitial: " + params.requestedLoadSize);
        mBaseQuery.limit(params.requestedLoadSize)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        List<DocumentSnapshot> data = snapshots.getDocuments();
                        DocumentSnapshot last = getLast(data);

                        PageKey nextPage = new PageKey(last, null);
                        callback.onResult(data, null, nextPage);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // TODO: errors?
                    }
                });

    }

    @Override
    public void loadBefore(@NonNull LoadParams<PageKey> params,
                           @NonNull LoadCallback<PageKey, DocumentSnapshot> callback) {
        PageKey key = params.key;
        Log.d(TAG, "loadBefore: " + key + ", " + params.requestedLoadSize);
        // TODO: Do I need the reverse query here?
    }

    @Override
    public void loadAfter(@NonNull LoadParams<PageKey> params,
                          @NonNull final LoadCallback<PageKey, DocumentSnapshot> callback) {
        PageKey key = params.key;
        Log.d(TAG, "loadAfter: " + key + ", " + params.requestedLoadSize);

        key.getPageQuery(mBaseQuery, params.requestedLoadSize)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        List<DocumentSnapshot> data = snapshots.getDocuments();
                        DocumentSnapshot last = getLast(data);

                        PageKey nextPage = new PageKey(last, null);
                        callback.onResult(data, nextPage);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

    }

    private DocumentSnapshot getLast(List<DocumentSnapshot> data) {
        DocumentSnapshot last = (data == null || data.isEmpty())
                ? null
                : data.get(data.size() - 1);

        return last;
    }
}
