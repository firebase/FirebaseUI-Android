package com.firebase.ui.database.adapter;

import android.app.Activity;
import android.support.annotation.LayoutRes;

import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.FirebaseIndexArray;
import com.google.firebase.database.Query;

public abstract class FirebaseIndexListAdapter<T> extends FirebaseListAdapter<T> {
    /**
     * @param keyRef  The Firebase location containing the list of keys to be found in {@code
     *                dataRef}. Can also be a slice of a location, using some combination of {@code
     *                limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef The Firebase location to watch for data changes. Each key key found in {@code
     *                keyRef}'s location represents a list item in the {@code ListView}.
     * @see FirebaseListAdapter#FirebaseListAdapter(Activity, FirebaseArray, Class, int)
     */
    public FirebaseIndexListAdapter(Activity activity,
                                    Class<T> modelClass,
                                    @LayoutRes int modelLayout,
                                    Query keyRef,
                                    Query dataRef) {
        super(activity, new FirebaseIndexArray(keyRef, dataRef), modelClass, modelLayout);
    }
}
