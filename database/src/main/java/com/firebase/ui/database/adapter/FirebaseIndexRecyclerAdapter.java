package com.firebase.ui.database.adapter;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.FirebaseIndexArray;
import com.google.firebase.database.Query;

public abstract class FirebaseIndexRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends FirebaseRecyclerAdapter<T, VH> {
    /**
     * @param keyRef  The Firebase location containing the list of keys to be found in {@code
     *                dataRef}. Can also be a slice of a location, using some combination of {@code
     *                limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef The Firebase location to watch for data changes. Each key key found at {@code
     *                keyRef}'s location represents a list item in the {@link RecyclerView}.
     * @see FirebaseRecyclerAdapter#FirebaseRecyclerAdapter(FirebaseArray, Class, Class, int)
     */
    public FirebaseIndexRecyclerAdapter(Class<T> modelClass,
                                        Class<VH> viewHolderClass,
                                        @LayoutRes int modelLayout,
                                        Query keyRef,
                                        Query dataRef) {
        super(new FirebaseIndexArray(keyRef, dataRef), modelClass, viewHolderClass, modelLayout);
    }
}
