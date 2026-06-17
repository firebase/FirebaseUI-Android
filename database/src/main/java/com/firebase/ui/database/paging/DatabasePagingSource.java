package com.firebase.ui.database.paging;

import android.annotation.SuppressLint;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.snapshot.Index;
import com.google.firebase.database.snapshot.PathIndex;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DatabasePagingSource extends RxPagingSource<DatabasePagingKey, DataSnapshot> {
    private final Query mQuery;

    private static final String STATUS_DATABASE_NOT_FOUND = "DATA_NOT_FOUND";
    private static final String MESSAGE_DATABASE_NOT_FOUND = "Data not found at given child path!";
    private static final String DETAILS_DATABASE_NOT_FOUND = "No data was returned for the given query: ";

    public DatabasePagingSource(Query query) {
        this.mQuery = query;
    }

    /**
     * DatabaseError.fromStatus() and PathIndex are not meant to be public.
     */
    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Single<LoadResult<DatabasePagingKey, DataSnapshot>> loadSingle(
            @NonNull LoadParams<DatabasePagingKey> params) {
        PathIndex pathIndex = getPathIndex();
        Task<DataSnapshot> task;

        if (params.getKey() == null) {
            task = mQuery.limitToFirst(params.getLoadSize()).get();
        } else {
            DatabasePagingKey key = params.getKey();
            if (pathIndex != null) {
                task = startAtChildValue(key.getChildValue(), key.getNodeKey())
                        .limitToFirst(params.getLoadSize() + 1).get();
            } else {
                task = mQuery.startAt(null, key.getNodeKey())
                        .limitToFirst(params.getLoadSize() + 1).get();
            }
        }

        return Single.fromCallable(() -> {
            try {
                Tasks.await(task);
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {
                    List<DataSnapshot> data = new ArrayList<>();
                    DatabasePagingKey lastKey = null;

                    if (params.getKey() == null) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            data.add(snapshot);
                        }
                    } else {
                        Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                        //Skip First Item
                        if (iterator.hasNext()) {
                            iterator.next();
                        }

                        while (iterator.hasNext()) {
                            data.add(iterator.next());
                        }
                    }

                    //Detect End of Data
                    if (!data.isEmpty()) {
                        DataSnapshot last = data.get(data.size() - 1);
                        Object childValue = pathIndex != null
                                ? getChildValue(last, pathIndex) : null;
                        lastKey = new DatabasePagingKey(childValue, last.getKey());
                    }
                    return toLoadResult(data, lastKey);
                } else {
                    String details = DETAILS_DATABASE_NOT_FOUND + mQuery.toString();
                    throw DatabaseError.fromStatus(
                            STATUS_DATABASE_NOT_FOUND,
                            MESSAGE_DATABASE_NOT_FOUND,
                            details).toException();
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
                throw new Exception(e);
            }
        }).subscribeOn(Schedulers.io()).onErrorReturn(LoadResult.Error::new);
    }

    @SuppressLint("RestrictedApi")
    private PathIndex getPathIndex() {
        Index index = mQuery.getSpec().getIndex();
        return index instanceof PathIndex ? (PathIndex) index : null;
    }

    @SuppressLint("RestrictedApi")
    private Object getChildValue(DataSnapshot snapshot, PathIndex pathIndex) {
        return snapshot.child(pathIndex.getQueryDefinition()).getValue();
    }

    @SuppressLint("RestrictedApi")
    private Query startAtChildValue(Object childValue, String nodeKey) {
        if (childValue instanceof String) {
            return mQuery.startAt((String) childValue, nodeKey);
        } else if (childValue instanceof Boolean) {
            return mQuery.startAt((Boolean) childValue, nodeKey);
        } else if (childValue instanceof Number) {
            return mQuery.startAt(((Number) childValue).doubleValue(), nodeKey);
        }
        return mQuery.startAt(null, nodeKey);
    }

    private LoadResult<DatabasePagingKey, DataSnapshot> toLoadResult(
            @NonNull List<DataSnapshot> snapshots,
            DatabasePagingKey nextPage
    ) {
        return new LoadResult.Page<>(
                snapshots,
                null, // Only paging forward.
                nextPage,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }

    @Nullable
    @Override
    public DatabasePagingKey getRefreshKey(
            @NonNull PagingState<DatabasePagingKey, DataSnapshot> state) {
        return null;
    }
}
