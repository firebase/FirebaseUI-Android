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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FirestorePagingSource extends RxPagingSource<PageKey, DocumentSnapshot> {

    // Workaround to show loading states in Paging 2
    // These can be removed once we fully migrate to Paging 3
    private final MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();
    private final MutableLiveData<Exception> mException = new MutableLiveData<>();

    private final Query mQuery;
    private final Source mSource;

    public FirestorePagingSource(@NonNull Query query, @NonNull Source source) {
        mQuery = query;
        mSource = source;
    }

    @NotNull
    @Override
    public Single<LoadResult<PageKey, DocumentSnapshot>> loadSingle(@NotNull LoadParams<PageKey> params) {
        final Task<QuerySnapshot> task;
        if (params.getKey() == null) {
            mLoadingState.postValue(LoadingState.LOADING_INITIAL);
            task = mQuery.limit(params.getLoadSize()).get(mSource);
        } else {
            mLoadingState.postValue(LoadingState.LOADING_MORE);
            task = params.getKey().getPageQuery(mQuery, params.getLoadSize()).get(mSource);
        }

        return Single.fromCallable(new Callable<LoadResult<PageKey, DocumentSnapshot>>() {
            @Override
            public LoadResult<PageKey, DocumentSnapshot> call() throws Exception {
                Tasks.await(task);
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    PageKey nextPage = getNextPageKey(snapshot);
                    mLoadingState.postValue(LoadingState.LOADED);
                    if (snapshot.getDocuments().isEmpty()) {
                        mLoadingState.postValue(LoadingState.FINISHED);
                        return toLoadResult(snapshot.getDocuments(), null);
                    }
                    return toLoadResult(snapshot.getDocuments(), nextPage);
                }
                mLoadingState.postValue(LoadingState.ERROR);
                throw task.getException();
            }
        }).subscribeOn(Schedulers.io())
                .onErrorReturn(new Function<Throwable, LoadResult<PageKey, DocumentSnapshot>>() {
                    @Override
                    public LoadResult<PageKey, DocumentSnapshot> apply(Throwable throwable) {
                        mLoadingState.postValue(LoadingState.ERROR);
                        mException.postValue((Exception) throwable);
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

    @NonNull
    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    @NonNull
    public LiveData<Exception> getLastError() {
        return mException;
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
