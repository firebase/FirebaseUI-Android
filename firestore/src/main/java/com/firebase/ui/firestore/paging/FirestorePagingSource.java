package com.firebase.ui.firestore.paging;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FirestorePagingSource extends RxPagingSource<PageKey, DocumentSnapshot> {

    private final Query mQuery;
    private final Source mSource;

    public FirestorePagingSource(Query query, Source source) {
        mQuery = query;
        mSource = source;
    }

    @NotNull
    @Override
    public Single<LoadResult<PageKey, DocumentSnapshot>> loadSingle(@NotNull LoadParams<PageKey> params) {
        final Task<QuerySnapshot> task;
        if (params.getKey() == null) {
            task = mQuery.limit(params.getLoadSize()).get(mSource);
        } else {
            task = params.getKey().getPageQuery(mQuery, params.getLoadSize()).get(mSource);
        }

        return Single.fromCallable(new Callable<LoadResult<PageKey, DocumentSnapshot>>() {
            @Override
            public LoadResult<PageKey, DocumentSnapshot> call() throws Exception {
                Tasks.await(task);
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    PageKey nextPage = getNextPageKey(snapshot);
                    if (snapshot.getDocuments().isEmpty()) {
                        return toLoadResult(snapshot.getDocuments(), null);
                    }
                    return toLoadResult(snapshot.getDocuments(), nextPage);
                }
                throw task.getException();
            }
        }).subscribeOn(Schedulers.io()).onErrorReturn(new Function<Throwable, LoadResult<PageKey, DocumentSnapshot>>() {
            @Override
            public LoadResult<PageKey, DocumentSnapshot> apply(Throwable throwable) {
                return new LoadResult.Error<>(throwable);
            }
        });
    }

    private LoadResult<PageKey, DocumentSnapshot> toLoadResult(
            @NonNull List<DocumentSnapshot> snapshots,
            @Nullable PageKey nextPage
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
    public PageKey getRefreshKey(@NotNull PagingState<PageKey, DocumentSnapshot> state) {
        return null;
    }

    @NonNull
    private PageKey getNextPageKey(@NonNull QuerySnapshot snapshot) {
        List<DocumentSnapshot> data = snapshot.getDocuments();
        DocumentSnapshot last = getLast(data);
        return new PageKey(last, null);
    }

    @androidx.annotation.Nullable
    private DocumentSnapshot getLast(@NonNull List<DocumentSnapshot> data) {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1);
        }
    }
}
