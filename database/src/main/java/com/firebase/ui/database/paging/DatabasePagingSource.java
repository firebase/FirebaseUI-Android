package com.firebase.ui.database.paging;

import android.annotation.SuppressLint;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.snapshot.Index;

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

public class DatabasePagingSource extends RxPagingSource<Object, DataSnapshot> {
    private final Query mQuery;

    private static final String STATUS_DATABASE_NOT_FOUND = "DATA_NOT_FOUND";
    private static final String MESSAGE_DATABASE_NOT_FOUND = "Data not found at given child path!";
    private static final String DETAILS_DATABASE_NOT_FOUND = "No data was returned for the given query: ";

    public DatabasePagingSource(Query query) {
        this.mQuery = query;
    }

    public Query startAt_childvalue(Object startvalue, String keyvalue) {
        if (startvalue instanceof String)
            return mQuery.startAt((String) startvalue, keyvalue);
        else if (startvalue instanceof Boolean)
            return mQuery.startAt((Boolean) startvalue, keyvalue);
        else if (startvalue instanceof Double)
            return mQuery.startAt((Double) startvalue, keyvalue);
        else if (startvalue instanceof Long)
            return mQuery.startAt(((Long) startvalue).doubleValue(), keyvalue);
        else
            return mQuery;
    }

    /**
     * DatabaseError.fromStatus() is not meant to be public.
     */
    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Single<LoadResult<Object, DataSnapshot>> loadSingle(@NonNull LoadParams<Object> params) {
        Task<DataSnapshot> task;

        Index queryChildPathIndex = mQuery.getSpec().getIndex();
        Pair<Object, String> pKey = (Pair<Object, String>) params.getKey();

        if (params.getKey() == null) {
            task = mQuery.limitToFirst(params.getLoadSize()).get();
        } else {
            //change mQuery.startAt at value  if child index
            //if not null then what we have here is orderByChild query
            if (queryChildPathIndex != null) {//orderByChild query mode
                task = startAt_childvalue(pKey.first, pKey.second)
                        .limitToFirst(params.getLoadSize() + 1)
                        .get();
            } else {
                task = mQuery.startAt(null, pKey.second)
                        .limitToFirst(params.getLoadSize() + 1)
                        .get();
            }
        }

        return Single.fromCallable(() -> {
            try {
                Tasks.await(task);
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {

                    //Make List of DataSnapshot
                    List<DataSnapshot> data = new ArrayList<>();
                    Pair<Object, String> lastKey = null;

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
                            DataSnapshot snapshot = iterator.next();
                            data.add(snapshot);
                        }
                    }

                    //Detect End of Data
                    if (!data.isEmpty()) {
                        //Get Last Key
                        Object lastkey_c = getLastPageChildKey(data, queryChildPathIndex);
                        String lastkey_k = getLastPageKey(data);
                        lastKey = (lastkey_c == null && lastkey_k == null)
                                ? null
                                : (lastkey_k == null) ? new Pair<>(lastkey_c, "") : new Pair<>(
                                lastkey_c,
                                lastkey_k);

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
                    // throw the original Exception
                    throw (Exception) e.getCause();
                }
                // Only throw a new Exception when the original
                // Throwable cannot be cast to Exception
                throw new Exception(e);
            }
        }).subscribeOn(Schedulers.io()).onErrorReturn(LoadResult.Error::new);
    }

    private LoadResult<Object, DataSnapshot> toLoadResult(
            @NonNull List<DataSnapshot> snapshots,
            Pair<Object, String> nextPage
    ) {
        return new LoadResult.Page<>(
                snapshots,
                null, // Only paging forward.
                nextPage,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
    }

    @SuppressLint("RestrictedApi")
    private Object getLastPageChildKey(@NonNull List<DataSnapshot> data, Index index) {
        if (index == null) return null;
        if (data.isEmpty()) {
            return null;
        } else {
            return getChildValue(data.get(data.size() - 1), index);
        }
    }

    @SuppressLint("RestrictedApi")
    private Object getChildValue(DataSnapshot snapshot, Index index) {
        String keypath = index.getQueryDefinition();
        DataSnapshot data = snapshot.child(keypath);
        if (!data.exists()) return null;
        return data.getValue();
    }

    @Nullable
    private String getLastPageKey(@NonNull List<DataSnapshot> data) {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1).getKey();
        }
    }

    @Nullable
    @Override
    public String getRefreshKey(@NonNull PagingState<Object, DataSnapshot> state) {
        return null;
    }
}
