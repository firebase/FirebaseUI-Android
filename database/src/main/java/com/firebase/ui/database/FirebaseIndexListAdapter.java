package com.firebase.ui.database;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public abstract class FirebaseIndexListAdapter<T> extends FirebaseListAdapter<T> {
    /**
     * @param parser   a custom {@link SnapshotParser} to convert a {@link DataSnapshot} to the
     *                 model class
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}. <b>Note, this can also be a
     *                 {@link DatabaseReference}.</b>
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link ListView}.
     * @see FirebaseListAdapter#FirebaseListAdapter(Context, ObservableSnapshotArray, int,
     * LifecycleOwner)
     */
    public FirebaseIndexListAdapter(Context context,
                                    SnapshotParser<T> parser,
                                    @LayoutRes int modelLayout,
                                    Query keyQuery,
                                    DatabaseReference dataRef,
                                    LifecycleOwner owner) {
        super(context, new FirebaseIndexArray<>(keyQuery, dataRef, parser), modelLayout, owner);
    }

    /**
     * @see #FirebaseIndexListAdapter(Context, SnapshotParser, int, Query, DatabaseReference,
     * LifecycleOwner)
     */
    public FirebaseIndexListAdapter(Context context,
                                    SnapshotParser<T> parser,
                                    @LayoutRes int modelLayout,
                                    Query keyQuery,
                                    DatabaseReference dataRef) {
        super(context, new FirebaseIndexArray<>(keyQuery, dataRef, parser), modelLayout);
    }

    /**
     * @see #FirebaseIndexListAdapter(Context, SnapshotParser, int, Query, DatabaseReference,
     * LifecycleOwner)
     */
    public FirebaseIndexListAdapter(Context context,
                                    Class<T> modelClass,
                                    @LayoutRes int modelLayout,
                                    Query keyQuery,
                                    DatabaseReference dataRef,
                                    LifecycleOwner owner) {
        this(context, new ClassSnapshotParser<>(modelClass), modelLayout, keyQuery, dataRef, owner);
    }

    /**
     * @see #FirebaseIndexListAdapter(Context, SnapshotParser, int, Query, DatabaseReference)
     */
    public FirebaseIndexListAdapter(Context context,
                                    Class<T> modelClass,
                                    @LayoutRes int modelLayout,
                                    Query keyQuery,
                                    DatabaseReference dataRef) {
        this(context, new ClassSnapshotParser<>(modelClass), modelLayout, keyQuery, dataRef);
    }
}
