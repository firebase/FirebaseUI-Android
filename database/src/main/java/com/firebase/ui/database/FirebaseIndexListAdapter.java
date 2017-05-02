package com.firebase.ui.database;

import android.app.Activity;
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
     * @see FirebaseIndexListAdapter#FirebaseIndexListAdapter(Activity, SnapshotParser, int, Query,
     * DatabaseReference)
     */
    public FirebaseIndexListAdapter(Activity activity,
                                    SnapshotParser<T> parser,
                                    @LayoutRes int modelLayout,
                                    Query keyQuery,
                                    DatabaseReference dataRef) {
        super(activity, new FirebaseIndexArray<>(keyQuery, dataRef, parser), modelLayout);
    }

    /**
     * @see #FirebaseIndexListAdapter(Activity, SnapshotParser, int, Query, DatabaseReference)
     */
    public FirebaseIndexListAdapter(Activity activity,
                                    Class<T> modelClass,
                                    @LayoutRes int modelLayout,
                                    Query keyQuery,
                                    DatabaseReference dataRef) {
        this(activity, new ClassSnapshotParser<>(modelClass), modelLayout, keyQuery, dataRef);
    }
}
