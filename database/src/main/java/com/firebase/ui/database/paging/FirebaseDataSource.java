package com.firebase.ui.database.paging;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.DataSource;
import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;

/**
 * Data source to power a {@link FirebaseRecyclerPagingAdapter}.
 *
 * Note: although loadInitial, loadBefore, and loadAfter are not called on the main thread by the
 *       paging library, we treat them as if they were so that we can facilitate retry without
 *       managing our own thread pool or requiring the user to pass us an executor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FirebaseDataSource<T> extends PageKeyedDataSource<String,T> {

    private Query mQuery;
    private Class<T> mClass;
    private ArrayList<String> mKeyList;

    private static final String TAG = "FirebaseDataSource";

    private final MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();
    private final MutableLiveData<DatabaseError> mError = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<String>> mKeyLiveData = new MutableLiveData<>();

    private final String WRONG_DATA_PATH_STATUS = "WRONG_PATH";
    private final String WRONG_DATA_PATH_MESSAGE = "WRONG DATA PATH";
    private final String WRONG_DATA_PATH_DETAILS = "Wrong Data Path is given. Data Child Not Found !";

    public static class Factory<T> extends DataSource.Factory<String, Class<T>> {

        private final Query mQuery;
        private final Class mClass;

        public Factory(@NonNull Query query, @NotNull Class<T> modelClass) {
            mQuery = query;
            mClass = modelClass;
        }

        @Override
        @NonNull
        public DataSource<String, Class<T>> create() {
            return new FirebaseDataSource(mQuery,mClass);
        }
    }

    FirebaseDataSource(Query mQuery, Class<T> modelClass){
        this.mQuery = mQuery;
        this.mClass = modelClass;
        mKeyList = new ArrayList<>();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params, @NonNull final LoadInitialCallback<String, T> callback) {
        // Set initial loading state
        mLoadingState.postValue(LoadingState.LOADING_INITIAL);

        Query mInitQuery = mQuery.limitToFirst(params.requestedLoadSize);
        mInitQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    ArrayList<T> mDataList = new ArrayList<T>();
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        T data = ds.getValue(mClass);
                        String key = ds.getKey();
                        mKeyList.add(key);
                        mDataList.add(data);
                    }

                    //Initial Load Success
                    mLoadingState.postValue(LoadingState.LOADED);
                    callback.onResult(mDataList, mKeyList.get(mKeyList.size() - 1), mKeyList.get(mKeyList.size() - 1));
                }
                else{
                    setWrongDataPathError();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Error Occured
                mError.postValue(databaseError);
                mLoadingState.postValue(LoadingState.ERROR);
            }
        });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<String> params, @NonNull LoadCallback<String, T> callback) {
        // Ignored for now, since we only ever append to the initial load.
    }

    @Override
    public void loadAfter(@NonNull LoadParams<String> params, @NonNull final LoadCallback<String, T> callback) {
        // Set loading state
        mLoadingState.postValue(LoadingState.LOADING_MORE);

        //Load params.requestedLoadSize+1 because, first data item is getting ignored.
        Query mNewQuery = mQuery.startAt(null,params.key).limitToFirst(params.requestedLoadSize+1);
        mNewQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    ArrayList<T> mList = new ArrayList<T>();
                    boolean isFirstItem = true;

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        T data = ds.getValue(mClass);
                        String key = ds.getKey();

                    /*
                      Check for first Item.
                      Because in Firebase Database there is no query for startAfter(key).
                      So we're ignoring first data item
                    */
                        if (!isFirstItem) {
                            mList.add(data);
                            mKeyList.add(key);
                        }
                        isFirstItem = false;
                    }

                    mLoadingState.postValue(LoadingState.LOADED);

                    //Detect End of Data
                    if (mList.isEmpty())
                        mLoadingState.postValue(LoadingState.FINISHED);

                    callback.onResult(mList, mKeyList.get(mKeyList.size() - 1));
                }
                else{
                   setWrongDataPathError();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mError.postValue(databaseError);
                mLoadingState.postValue(LoadingState.ERROR);
            }
        });
    }

    private void setWrongDataPathError(){
        mError.postValue(DatabaseError.fromStatus(
                WRONG_DATA_PATH_STATUS,
                WRONG_DATA_PATH_DETAILS,
                WRONG_DATA_PATH_MESSAGE));

        mLoadingState.postValue(LoadingState.ERROR);
    }

    @NonNull
    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    public LiveData<ArrayList<String>> getKeyList(){
        mKeyLiveData.postValue(mKeyList);
        return mKeyLiveData;
    }

    public LiveData<DatabaseError> getLastError(){
        return mError;
    }
}
