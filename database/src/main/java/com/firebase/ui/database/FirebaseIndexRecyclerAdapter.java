package com.firebase.ui.database;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public abstract class FirebaseIndexRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends FirebaseRecyclerAdapter<T, VH> {
    /**
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link RecyclerView}.
     * @see FirebaseRecyclerAdapter#FirebaseRecyclerAdapter(ObservableSnapshotArray, Class, int,
     * Class)
     */
    public FirebaseIndexRecyclerAdapter(Class<T> modelClass,
                                        @LayoutRes int modelLayout,
                                        Class<VH> viewHolderClass,
                                        Query keyQuery,
                                        DatabaseReference dataRef) {
        init(new FirebaseIndexArray<>(keyQuery, dataRef, this),
             modelClass,
             modelLayout,
             viewHolderClass);
    }
}
