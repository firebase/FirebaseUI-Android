package com.firebase.ui.database.paging;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

/**
 * Data source to power a {@link FirebaseRecyclerPagingAdapter}.
 *
 * Note: although loadInitial, loadBefore, and loadAfter are not called on the main thread by the
 *       paging library, we treat them as if they were so that we can facilitate retry without
 *       managing our own thread pool or requiring the user to pass us an executor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseDataSource extends PageKeyedDataSource<String, DataSnapshot> {
    private static final String TAG = "FirebaseDataSource";

    private Query mQuery;

    private final MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();
    private final MutableLiveData<DatabaseError> mError = new MutableLiveData<>();

    private static final String STATUS_DATABASE_NOT_FOUND = "DATA_NOT_FOUND";
    private static final String MESSAGE_DATABASE_NOT_FOUND = "Data not found at given child path!";
    private static final String DETAILS_DATABASE_NOT_FOUND = "No data was returned for the given query: ";

    private Runnable mRetryRunnable;

    public static class Factory extends DataSource.Factory<String, DataSnapshot> {

        private final Query mQuery;

        public Factory(@NonNull Query query) {
            mQuery = query;
        }

        @Override
        @NonNull
        public DataSource<String, DataSnapshot> create() {
            return new FirebaseDataSource(mQuery);
        }
    }

    FirebaseDataSource(Query mQuery){
        this.mQuery = mQuery;
    }

    @Override
    public void loadInitial(@NonNull final LoadInitialParams<String> params,
                            @NonNull final LoadInitialCallback<String, DataSnapshot> callback) {

        // Set initial loading state
        mLoadingState.postValue(LoadingState.LOADING_INITIAL);

        Query mInitQuery = mQuery.limitToFirst(params.requestedLoadSize);
        mInitQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    //Make List of DataSnapshot
                    List<DataSnapshot> data = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                        data.add(snapshot);
                    }

                    //Get Last Key
                    String lastKey = getLastPageKey(data);

                    //Update State
                    mLoadingState.postValue(LoadingState.LOADED);
                    mRetryRunnable = null;

                    callback.onResult(data, lastKey, lastKey);

                } else {
                    mRetryRunnable = getRetryLoadInitial(params, callback);
                    setDatabaseNotFoundError();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mRetryRunnable = getRetryLoadInitial(params, callback);
                setError(databaseError);
            }
        });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<String, DataSnapshot> callback) {
        // Ignored for now, since we only ever append to the initial load.
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<String> params,
                          @NonNull final LoadCallback<String, DataSnapshot> callback) {

        // Set loading state
        mLoadingState.postValue(LoadingState.LOADING_MORE);

        //Load params.requestedLoadSize+1 because, first data item is getting ignored.
        Query mNewQuery = mQuery.startAt(null, params.key).limitToFirst(params.requestedLoadSize + 1);
        mNewQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    //Make List of DataSnapshot
                    List<DataSnapshot> data = new ArrayList<>();
                    String lastKey = null;

                    Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                    //Skip First Item
                    if (iterator.hasNext()) {
                        iterator.next();
                    }

                    while (iterator.hasNext()) {
                        DataSnapshot snapshot = iterator.next();
                        data.add(snapshot);
                    }

                    //Update State
                    mLoadingState.postValue(LoadingState.LOADED);
                    mRetryRunnable = null;

                    //Detect End of Data
                    if (data.isEmpty())
                        mLoadingState.postValue(LoadingState.FINISHED);
                    else {
                        //Get Last Key
                        lastKey = getLastPageKey(data);
                    }

                    callback.onResult(data, lastKey);

                } else {
                    mRetryRunnable = getRetryLoadAfter(params, callback);
                    setDatabaseNotFoundError();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mRetryRunnable = getRetryLoadAfter(params, callback);
                setError(databaseError);
            }
        });
    }

    @NonNull
    private Runnable getRetryLoadAfter(@NonNull final LoadParams<String> params,
                                       @NonNull final LoadCallback<String, DataSnapshot> callback) {
        return new Runnable() {
            @Override
            public void run() {
                loadAfter(params, callback);
            }
        };
    }

    @NonNull
    private Runnable getRetryLoadInitial(@NonNull final LoadInitialParams<String> params,
                                         @NonNull final LoadInitialCallback<String, DataSnapshot> callback) {
        return new Runnable() {
            @Override
            public void run() {
                loadInitial(params, callback);
            }
        };
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
    private String getLastPageKey(@NonNull List<DataSnapshot> data) {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1).getKey();
        }
    }

    /**
     * DatabaseError.fromStatus() is not meant to be public.
     */
    @SuppressLint("RestrictedApi")
    private void setDatabaseNotFoundError(){
        String details = DETAILS_DATABASE_NOT_FOUND + mQuery.toString();
        mError.postValue(DatabaseError.fromStatus(
                STATUS_DATABASE_NOT_FOUND,
                MESSAGE_DATABASE_NOT_FOUND,
                details));

        mLoadingState.postValue(LoadingState.ERROR);
    }

    private void setError(DatabaseError databaseError){
        mError.postValue(databaseError);
        mLoadingState.postValue(LoadingState.ERROR);
    }

    @NonNull
    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    @NonNull
    public LiveData<DatabaseError> getLastError(){
        return mError;
    }

}
