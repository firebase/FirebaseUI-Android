package com.firebase.ui.database.paging;

import android.annotation.SuppressLint;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;

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

public class DatabasePagingSource extends RxPagingSource<String, DataSnapshot> {
    private final Query mQuery;

    private static final String STATUS_DATABASE_NOT_FOUND = "DATA_NOT_FOUND";
    private static final String MESSAGE_DATABASE_NOT_FOUND = "Data not found at given child path!";
    private static final String DETAILS_DATABASE_NOT_FOUND = "No data was returned for the given query: ";

    public DatabasePagingSource(Query query) {
        this.mQuery = query;
    }

    /**
     * DatabaseError.fromStatus() is not meant to be public.
     */
    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Single<LoadResult<String, DataSnapshot>> loadSingle(@NonNull LoadParams<String> params) {
        Task<DataSnapshot> task;
        if (params.getKey() == null) {
            task = mQuery.limitToFirst(params.getLoadSize()).get();
        } else {
            task = mQuery.startAt(null, params.getKey()).limitToFirst(params.getLoadSize() + 1).get();
        }

        return Single.fromCallable(() -> {
            try {
                Tasks.await(task);
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {

                    //Make List of DataSnapshot
                    List<DataSnapshot> data = new ArrayList<>();
                    String lastKey = null;

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
                        lastKey = getLastPageKey(data);
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
                throw new Exception(e.getCause());
            }
        }).subscribeOn(Schedulers.io()).onErrorReturn(LoadResult.Error::new);
    }

    private LoadResult<String, DataSnapshot> toLoadResult(
            @NonNull List<DataSnapshot> snapshots,
            String nextPage
    ) {
        return new LoadResult.Page<>(
                snapshots,
                null, // Only paging forward.
                nextPage,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED);
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
    public String getRefreshKey(@NonNull PagingState<String, DataSnapshot> state) {
        return null;
    }
}
