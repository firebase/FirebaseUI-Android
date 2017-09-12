package com.firebase.ui.database;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import static com.firebase.ui.common.Preconditions.assertNonNull;
import static com.firebase.ui.common.Preconditions.assertNull;

/**
 * Options to configure a {@link FirebaseRecyclerAdapter}.
 *
 * @see Builder
 */
public class FirebaseRecyclerOptions<T, VH extends RecyclerView.ViewHolder> {

    private static final String ERR_SNAPSHOTS_SET = "Snapshot array already set. " +
            "Call only one of setSnapshotArray, setQuery, or setIndexedQuery.";

    private static final String ERR_SNAPSHOTS_NULL =  "Snapshot array cannot be null. " +
            "Call one of setSnapshotArray, setQuery, or setIndexedQuery.";

    private static final String ERR_VIEWHOLDER_NULL = "View holder class cannot be null. " +
            "Call setViewHolder.";

    private final ObservableSnapshotArray<T> mSnapshots;
    private final Class<VH> mViewHolderClass;
    private final int mModelLayout;
    private final LifecycleOwner mOwner;

    private FirebaseRecyclerOptions(ObservableSnapshotArray<T> snapshots,
                                    Class<VH> viewHolderClass,
                                    @LayoutRes int modelLayout,
                                    LifecycleOwner owner) {
        mSnapshots = snapshots;
        mViewHolderClass = viewHolderClass;
        mModelLayout = modelLayout;
        mOwner = owner;
    }

    /**
     * Get the {@link ObservableSnapshotArray} to listen to.
     */
    public ObservableSnapshotArray<T> getSnapshots() {
        return mSnapshots;
    }

    /**
     * Get the class of the {@link android.support.v7.widget.RecyclerView.ViewHolder} for
     * each RecyclerView item.
     */
    public Class<VH> getViewHolderClass() {
        return mViewHolderClass;
    }

    /**
     * Get the resource ID of the layout for each RecyclerView item.
     */
    @LayoutRes
    public int getModelLayout() {
        return mModelLayout;
    }

    /**
     * Get the (optional) LifecycleOwner. Listening will start/stop after the appropriate
     * lifecycle events.
     */
    @Nullable
    public LifecycleOwner getOwner() {
        return mOwner;
    }

    /**
     * Builder for a {@link FirebaseRecyclerOptions}.
     *
     * @param <T> the model class for the {@link FirebaseRecyclerAdapter}.
     * @param <VH> the ViewHolder class for the {@link FirebaseRecyclerAdapter}.
     */
    public static class Builder<T, VH extends RecyclerView.ViewHolder> {

        private ObservableSnapshotArray<T> mSnapshots;
        private Class<VH> mViewHolderClass;
        private int mModelLayout;
        private LifecycleOwner mOwner;

        /**
         * Directly set the {@link ObservableSnapshotArray} to be listened to.
         *
         * Do not call this method after calling {@code setQuery}.
         */
        public Builder<T, VH> setSnapshotArray(ObservableSnapshotArray<T> snapshots) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = snapshots;
            return this;
        }

        /**
         * Set the Firebase query to listen to, along with a {@link SnapshotParser} to
         * parse snapshots into model objects.
         *
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        public Builder<T, VH> setQuery(Query query, SnapshotParser<T> snapshotParser) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = new FirebaseArray<T>(query, snapshotParser);
            return this;
        }

        /**
         * Set the Firebase query to listen to, along with a {@link Class} to which snapshots
         * should be parsed.
         *
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        public Builder<T, VH> setQuery(Query query, Class<T> modelClass) {
            return setQuery(query, new ClassSnapshotParser<T>(modelClass));
        }


        /**
         * Set an indexed Firebase query to listen to, along with a {@link SnapshotParser} to
         * parse snapshots into model objects. Keys are identified by the {@code keyQuery} and then
         * data is fetched using those keys from the {@code dataRef}.
         *
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        public Builder<T, VH> setIndexedQuery(Query keyQuery,
                                              DatabaseReference dataRef,
                                              SnapshotParser<T> snapshotParser) {
            assertNull(mSnapshots, ERR_SNAPSHOTS_SET);

            mSnapshots = new FirebaseIndexArray<T>(keyQuery, dataRef, snapshotParser);
            return this;
        }

        /**
         * Set an indexed Firebase query to listen to, along with a {@link Class} to which
         * snapshots should be parsed. Keys are identified by the {@code keyQuery} and then
         * data is fetched using those keys from the {@code dataRef}.
         *
         * Do not call this method after calling {@link #setSnapshotArray(ObservableSnapshotArray)}.
         */
        public Builder<T, VH> setIndexedQuery(Query keyQuery,
                                              DatabaseReference dataRef,
                                              Class<T> modelClass) {
            return setIndexedQuery(keyQuery, dataRef, new ClassSnapshotParser<T>(modelClass));
        }

        /**
         * Set the layout resource ID and class for the
         * {@link android.support.v7.widget.RecyclerView.ViewHolder} for each RecyclerView item.
         */
        public Builder<T, VH> setViewHolder(@LayoutRes int modelLayout, Class<VH> viewHolderClass) {
            mModelLayout = modelLayout;
            mViewHolderClass = viewHolderClass;

            return this;
        }

        /**
         * Set the (optional) {@link LifecycleOwner}. Listens will start and stop after the
         * appropriate lifecycle events.
         */
        public Builder<T, VH> setLifecycleOwner(LifecycleOwner owner) {
            mOwner = owner;
            return this;
        }

        /**
         * Build a {@link FirebaseRecyclerOptions} from the provided arguments.
         */
        public FirebaseRecyclerOptions<T, VH> build() {
            assertNonNull(mSnapshots, ERR_SNAPSHOTS_NULL);
            assertNonNull(mViewHolderClass, ERR_VIEWHOLDER_NULL);

            return new FirebaseRecyclerOptions<>(
                    mSnapshots, mViewHolderClass, mModelLayout, mOwner);
        }
    }

}
