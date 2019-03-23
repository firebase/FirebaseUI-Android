package com.firebase.ui.database.paging;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.DataSource;
import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private static final String STATUS_DATABASE_NOT_FOUND = "DATABASE NOT FOUND";
    private static final String MESSAGE_DATABASE_NOT_FOUND = "Database not found at given child path !";
    private static final String DETAILS_DATABASE_NOT_FOUND = "Database Children Not Found in the specified child path. Please specify correct child path/reference";

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

                    callback.onResult(data, lastKey, lastKey);

                } else {
                    setDatabaseNotFoundError();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
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
                    if (iterator.hasNext())
                        iterator.next();

                    while (iterator.hasNext()) {
                        DataSnapshot snapshot = iterator.next();
                        data.add(snapshot);
                    }

                    //Update State
                    mLoadingState.postValue(LoadingState.LOADED);

                    //Detect End of Data
                    if (data.isEmpty())
                        mLoadingState.postValue(LoadingState.FINISHED);
                    else {
                        //Get Last Key
                        lastKey = getLastPageKey(data);
                    }

                    callback.onResult(data, lastKey);

                } else {
                   setDatabaseNotFoundError();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                setError(databaseError);
            }
        });
    }

    @Nullable
    private String getLastPageKey(@NonNull List<DataSnapshot> data) {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1).getKey();
        }
    }

    private void setDatabaseNotFoundError(){
        mError.postValue(DatabaseError.fromStatus(
                STATUS_DATABASE_NOT_FOUND,
                DETAILS_DATABASE_NOT_FOUND,
                MESSAGE_DATABASE_NOT_FOUND));

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
