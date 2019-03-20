package com.firebase.ui.database.paging;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import com.google.firebase.database.Query;

/**
 * Options to configure an {@link FirebaseRecyclerPagingAdapter}.
 *
 * Use {@link Builder} to create a new instance.
 */
public final class FirebasePagingOptions<T> {

    private final LiveData<PagedList<T>> mData;
    private final DiffUtil.ItemCallback<T> mDiffCallback;
    private final LifecycleOwner mOwner;

    private FirebasePagingOptions(@NonNull LiveData<PagedList<T>> data,
                                  @NonNull DiffUtil.ItemCallback<T> diffCallback,
                                  @Nullable LifecycleOwner owner) {
        mData = data;
        mDiffCallback = diffCallback;
        mOwner = owner;
    }

    @NonNull
    public LiveData<PagedList<T>> getData() {
        return mData;
    }

    @NonNull
    public DiffUtil.ItemCallback<T> getDiffCallback() {
        return mDiffCallback;
    }

    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    /**
     * Builder for {@link FirebasePagingOptions}.
     */
    public static final class Builder<T> {

        private LiveData<PagedList<T>> mData;
        private LifecycleOwner mOwner;
        private DiffUtil.ItemCallback<T> mDiffCallback;

        /**
         * Sets the Firestore query to paginate.
         *
         * @param query the FirebaseDatabase query. This query should only contain orderByKey(), orderByChild() and
         *              orderByValue() clauses. Any limit will cause an error such as limitToLast() or limitToFirst().
         * @param config paging configuration, passed directly to the support paging library.
         * @param modelClass the model class of data to parse into object.
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setQuery(@NonNull Query query, @NonNull PagedList.Config config, @NonNull Class<T> modelClass) {
            FirebaseDataSource.Factory factory = new FirebaseDataSource.Factory(query, modelClass);
            mData = new LivePagedListBuilder<>(factory, config).build();
            return this;
        }

        /**
         * Sets an optional custom {@link DiffUtil.ItemCallback} to compare
         * {@link T} objects.
         *
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setDiffCallback(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
            mDiffCallback = diffCallback;
            return this;
        }


        /**
         * Sets an optional {@link LifecycleOwner} to control the lifecycle of the adapter. Otherwise,
         * you must manually call {@link FirebaseRecyclerPagingAdapter#startListening()}
         * and {@link FirebaseRecyclerPagingAdapter#stopListening()}.
         *
         * @return this, for chaining.
         */
        @NonNull
        public Builder<T> setLifecycleOwner(@NonNull LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        /**
         * Build the {@link FirebasePagingOptions} object.
         */
        @NonNull
        public FirebasePagingOptions<T> build() {
            if (mData == null) {
                throw new IllegalStateException("Must call setQuery() before calling build().");
            }

            if (mDiffCallback == null) {
                mDiffCallback = new DiffUtil.ItemCallback<T>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                        return oldItem.equals(newItem);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
                        return oldItem.equals(newItem);
                    }
                };
            }

            return new FirebasePagingOptions<>(mData, mDiffCallback, mOwner);
        }

    }
}
