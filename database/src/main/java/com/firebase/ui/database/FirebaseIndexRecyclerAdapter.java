package com.firebase.ui.database;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public abstract class FirebaseIndexRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends FirebaseRecyclerAdapter<T, VH> {
    /**
     * @param parser   a custom {@link SnapshotParser} to convert a {@link DataSnapshot} to the
     *                 model class
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link RecyclerView}.
     * @see FirebaseRecyclerAdapter#FirebaseRecyclerAdapter(ObservableSnapshotArray, int, Class)
     */
    public FirebaseIndexRecyclerAdapter(SnapshotParser<T> parser,
                                        @LayoutRes int modelLayout,
                                        Class<VH> viewHolderClass,
                                        Query keyQuery,
                                        DatabaseReference dataRef) {
        super(new FirebaseIndexArray<>(keyQuery, dataRef, parser), modelLayout, viewHolderClass);
    }

    /**
     * @see #FirebaseIndexRecyclerAdapter(SnapshotParser, int, Class, Query, DatabaseReference)
     */
    public FirebaseIndexRecyclerAdapter(Class<T> modelClass,
                                        @LayoutRes int modelLayout,
                                        Class<VH> viewHolderClass,
                                        Query keyQuery,
                                        DatabaseReference dataRef) {
        this(new ClassSnapshotParser<>(modelClass),
             modelLayout,
             viewHolderClass,
             keyQuery,
             dataRef);
    }
}
