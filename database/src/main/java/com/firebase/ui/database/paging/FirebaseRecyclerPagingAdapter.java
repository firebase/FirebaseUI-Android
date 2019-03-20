package com.firebase.ui.database.paging;

import android.arch.core.util.Function;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.Transformations;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.paging.listener.StateChangedListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;

/**
 * Paginated RecyclerView Adapter for a Firebase Realtime Database query.
 *
 * Configured with {@link FirebasePagingOptions}.
 */
public abstract class FirebaseRecyclerPagingAdapter<T,VH extends RecyclerView.ViewHolder> extends PagedListAdapter<T,VH> implements LifecycleObserver{

    private final String TAG = "FirebaseRecyclerPagingAdapter";

    private final LiveData<PagedList<T>> mPagedList;
    private final LiveData<LoadingState> mLoadingState;
    private final LiveData<ArrayList<String>> mKeyListLiveData;
    private final LiveData<DatabaseError> mDatabaseError;
    private final LiveData<FirebaseDataSource> mDataSource;

    private StateChangedListener mListener;
    private ArrayList<String> mKeyList;
    private DatabaseError mLastError;

    //State Observer
    private final Observer<LoadingState> mStateObserver = new Observer<LoadingState>() {
        @Override
        public void onChanged(@Nullable LoadingState state) {
            if (state == null || mListener == null) {
                return;
            }

            switch (state){
                case LOADING_INITIAL: mListener.onInitLoading(); break;
                case LOADED: mListener.onLoaded(); break;
                case LOADING_MORE: mListener.onLoading(); break;
                case FINISHED: mListener.onFinished(); break;
                case ERROR: mListener.onError(mLastError);break;
            }
        }
    };

    //Data Observer
    private final Observer<PagedList<T>> mDataObserver = new Observer<PagedList<T>>() {
        @Override
        public void onChanged(@Nullable PagedList<T> snapshots) {
            if (snapshots == null) {
                return;
            }
            submitList(snapshots);
        }
    };

    //Item Keys Observer
    private final Observer<ArrayList<String>> mKeysObserver = new Observer<ArrayList<String>>() {
        @Override
        public void onChanged(@Nullable ArrayList<String> keyList) {
            mKeyList = keyList;
        }
    };

    //DatabaseError Observer
    private final Observer<DatabaseError> mErrorObserver = new Observer<DatabaseError>() {
        @Override
        public void onChanged(@Nullable DatabaseError databaseError) {
            mLastError = databaseError;
        }
    };

    /**
     * Construct a new FirestorePagingAdapter from the given {@link FirebasePagingOptions}.
     */
    public FirebaseRecyclerPagingAdapter(FirebasePagingOptions options){
        super(options.getDiffCallback());

        mPagedList = options.getData();

        //Init Data Source
        mDataSource = Transformations.map(mPagedList,
                new Function<PagedList<T>, FirebaseDataSource>() {
                    @Override
                    public FirebaseDataSource apply(PagedList<T> input) {
                        return (FirebaseDataSource) input.getDataSource();
                    }
                });

        //Init Loading State
        mLoadingState = Transformations.switchMap(mPagedList,
                new Function<PagedList<T>, LiveData<LoadingState>>() {
                    @Override
                    public LiveData<LoadingState> apply(PagedList<T> input) {
                        FirebaseDataSource dataSource = (FirebaseDataSource) input.getDataSource();
                        return dataSource.getLoadingState();
                    }
                });

        //Init Key List
        mKeyListLiveData = Transformations.switchMap(mPagedList,
                new Function<PagedList<T>, LiveData<ArrayList<String>>>() {
                    @Override
                    public LiveData<ArrayList<String>> apply(PagedList<T> input) {
                        FirebaseDataSource dataSource = (FirebaseDataSource) input.getDataSource();
                        return dataSource.getKeyList();
                    }
                });

        //Init Database Error
        mDatabaseError = Transformations.switchMap(mPagedList,
                new Function<PagedList<T>, LiveData<DatabaseError>>() {
                    @Override
                    public LiveData<DatabaseError> apply(PagedList<T> input) {
                        FirebaseDataSource dataSource = (FirebaseDataSource) input.getDataSource();
                        return dataSource.getLastError();
                    }
                });

        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }

    }

    /**
     * Start listening to paging / scrolling events and populating adapter data.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        mPagedList.observeForever(mDataObserver);
        mLoadingState.observeForever(mStateObserver);
        mKeyListLiveData.observeForever(mKeysObserver);
        mDatabaseError.observeForever(mErrorObserver);
    }

    /**
     * Unsubscribe from paging / scrolling events, no more data will be populated, but the existing
     * data will remain.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mPagedList.removeObserver(mDataObserver);
        mLoadingState.removeObserver(mStateObserver);
        mKeyListLiveData.removeObserver(mKeysObserver);
        mDatabaseError.removeObserver(mErrorObserver);
    }

    @Override
    public void onBindViewHolder(@NonNull VH viewHolder, int position) {
        T model = getItem(position);
        String key = mKeyList.get(position);
        onBindViewHolder(viewHolder, position, key, model);
    }

    /**
     * @param model the model object containing the data that should be used to populate the view.
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    protected abstract void onBindViewHolder(@NonNull VH viewHolder, int position, @NotNull String key, @NotNull T model);

    /**
     * Called whenever the loading state of the adapter changes.
     *
     * When the state is {@link LoadingState#ERROR} the adapter will stop loading any data
     */
    public void setStateChangedListener(StateChangedListener mListener){
        this.mListener = mListener;
    }

}
