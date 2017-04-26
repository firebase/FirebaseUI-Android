/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.database;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseIndexArray<T> extends CachingObservableSnapshotArray<T> implements ChangeEventListener {
    private static final String TAG = "FirebaseIndexArray";

    private DatabaseReference mDataRef;
    private Map<DatabaseReference, ValueEventListener> mRefs = new HashMap<>();

    private FirebaseArray<String> mKeySnapshots;
    private JoinResolver mJoinResolver;
    private List<DataSnapshot> mDataSnapshots = new ArrayList<>();

    /**
     * When keys are added in {@link FirebaseArray}, we need to fetch the data async. This list
     * contains keys that exist in the backing {@link FirebaseArray}, but their data hasn't been
     * downloaded yet in this array.
     */
    private List<String> mKeysWithPendingData = new ArrayList<>();
    /**
     * Moves or deletions don't need to fetch new data so they can be performed instantly once the
     * backing {@link FirebaseArray} is done updating. This will be true if the backing {@link
     * FirebaseArray} is in the middle of an update, false otherwise.
     */
    private boolean mHasPendingMoveOrDelete;

    /**
     * Create a new FirebaseIndexArray with a custom {@link SnapshotParser} and {@link
     * JoinResolver}.
     *
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link RecyclerView}.
     * @see ObservableSnapshotArray#ObservableSnapshotArray(SnapshotParser)
     */
    public FirebaseIndexArray(Query keyQuery,
                              DatabaseReference dataRef,
                              SnapshotParser<T> parser,
                              JoinResolver resolver) {
        super(parser);
        init(keyQuery, dataRef, resolver);
    }

    /**
     * Create a new FirebaseIndexArray that parses snapshots as members of a given class and joins
     * refs together with a custom {@link JoinResolver}.
     *
     * @see FirebaseIndexArray#FirebaseIndexArray(Query, DatabaseReference, SnapshotParser,
     * JoinResolver)
     * @see ObservableSnapshotArray#ObservableSnapshotArray(Class)
     */
    public FirebaseIndexArray(Query keyQuery,
                              DatabaseReference dataRef,
                              Class<T> tClass,
                              JoinResolver resolver) {
        super(tClass);
        init(keyQuery, dataRef, resolver);
    }

    /**
     * Create a new FirebaseIndexArray that parses snapshots as members of a given class.
     *
     * @see FirebaseIndexArray#FirebaseIndexArray(Query, DatabaseReference, Class, JoinResolver)
     * @see ObservableSnapshotArray#ObservableSnapshotArray(Class)
     */
    public FirebaseIndexArray(Query keyQuery, DatabaseReference dataRef, Class<T> tClass) {
        super(tClass);
        init(keyQuery, dataRef);
    }

    /**
     * Create a new FirebaseIndexArray with a custom {@link SnapshotParser}.
     *
     * @see FirebaseIndexArray#FirebaseIndexArray(Query, DatabaseReference, SnapshotParser,
     * JoinResolver)
     * @see ObservableSnapshotArray#ObservableSnapshotArray(SnapshotParser)
     */
    public FirebaseIndexArray(Query keyQuery, DatabaseReference dataRef, SnapshotParser<T> parser) {
        super(parser);
        init(keyQuery, dataRef);
    }

    @CallSuper
    protected void init(Query keyQuery, DatabaseReference dataRef) {
        init(keyQuery, dataRef, new DefaultJoinResolver());
    }

    @CallSuper
    protected void init(Query keyQuery, DatabaseReference dataRef, JoinResolver resolver) {
        mDataRef = dataRef;
        mJoinResolver = resolver;
        mKeySnapshots = new FirebaseArray<>(keyQuery, new SnapshotParser<String>() {
            @Override
            public String parseSnapshot(DataSnapshot snapshot) {
                return snapshot.getKey();
            }
        });

        mKeySnapshots.addChangeEventListener(this);
    }

    @Override
    public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
        switch (type) {
            case ADDED:
                onKeyAdded(snapshot);
                break;
            case MOVED:
                onKeyMoved(snapshot, index, oldIndex);
                break;
            case CHANGED:
                // This is a no-op, we don't care when a key 'changes' since that should not
                // be a supported operation
                break;
            case REMOVED:
                onKeyRemoved(snapshot, index);
                break;
        }
    }

    @Override
    public void onDataChanged() {
        if (mHasPendingMoveOrDelete) {
            notifyListenersOnDataChanged();
            mHasPendingMoveOrDelete = false;
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
    }

    @Override
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        super.removeChangeEventListener(listener);
        if (!isListening()) {
            for (DatabaseReference ref : mRefs.keySet()) {
                ref.removeEventListener(mRefs.get(ref));
            }

            clearData();
        }
    }

    @Override
    protected List<DataSnapshot> getSnapshots() {
        return mDataSnapshots;
    }

    @Override
    protected void clearData() {
        super.clearData();
        mRefs.clear();
    }

    private int getIndexForKey(String key) {
        int dataCount = size();
        int index = 0;
        for (int keyIndex = 0; index < dataCount; keyIndex++) {
            String superKey = mKeySnapshots.getObject(keyIndex);
            if (key.equals(superKey)) {
                break;
            } else if (mDataSnapshots.get(index).getKey().equals(superKey)) {
                index++;
            }
        }
        return index;
    }

    /**
     * Determines if a DataSnapshot with the given key is present at the given index.
     */
    private boolean isKeyAtIndex(String key, int index) {
        return index >= 0 && index < size() && mDataSnapshots.get(index).getKey().equals(key);
    }

    protected void onKeyAdded(DataSnapshot data) {
        DatabaseReference ref = mJoinResolver.onJoin(data);

        mKeysWithPendingData.add(data.getKey());
        // Start listening
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener()));
    }

    protected void onKeyMoved(DataSnapshot data, int index, int oldIndex) {
        String key = data.getKey();

        if (isKeyAtIndex(key, oldIndex)) {
            DataSnapshot snapshot = removeData(oldIndex);
            mHasPendingMoveOrDelete = true;
            mDataSnapshots.add(index, snapshot);
            notifyChangeEventListeners(EventType.MOVED, snapshot, index, oldIndex);
        }
    }

    protected void onKeyRemoved(DataSnapshot data, int index) {
        DatabaseReference removeRef = mJoinResolver.onDisjoin(data);
        ValueEventListener listener = mRefs.remove(removeRef);
        if (listener != null) removeRef.removeEventListener(listener);

        if (isKeyAtIndex(data.getKey(), index)) {
            DataSnapshot snapshot = removeData(index);
            mHasPendingMoveOrDelete = true;
            notifyChangeEventListeners(EventType.REMOVED, snapshot, index);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;

        FirebaseIndexArray array = (FirebaseIndexArray) obj;

        return mDataRef.equals(array.mDataRef) && mDataSnapshots.equals(array.mDataSnapshots);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mDataRef.hashCode();
        result = 31 * result + mDataSnapshots.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (isListening()) {
            return "FirebaseIndexArray is listening at " + mDataRef + ":\n" + mDataSnapshots;
        } else {
            return "FirebaseIndexArray is inactive";
        }
    }

    /**
     * A ValueEventListener attached to the joined child data.
     */
    protected class DataRefListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            String key = snapshot.getKey();
            int index = getIndexForKey(key);

            if (snapshot.getValue() != null) {
                if (isKeyAtIndex(key, index)) {
                    // We already know about this data, just update it
                    updateData(index, snapshot);
                    notifyChangeEventListeners(EventType.CHANGED, snapshot, index);
                    notifyListenersOnDataChanged();
                } else {
                    // We don't already know about this data, add it
                    mDataSnapshots.add(index, snapshot);
                    notifyChangeEventListeners(EventType.ADDED, snapshot, index);

                    mKeysWithPendingData.remove(key);
                    if (mKeysWithPendingData.isEmpty()) notifyListenersOnDataChanged();
                }
            } else {
                if (isKeyAtIndex(key, index)) {
                    // This data has disappeared, remove it
                    removeData(index);
                    notifyChangeEventListeners(EventType.REMOVED, snapshot, index);
                    notifyListenersOnDataChanged();
                } else {
                    // Data does not exist
                    mJoinResolver.onJoinFailed(snapshot, index);
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            notifyListenersOnCancelled(error);
        }
    }

    protected class DefaultJoinResolver implements JoinResolver {
        @NonNull
        @Override
        public DatabaseReference onJoin(DataSnapshot keySnapshot) {
            return mDataRef.child(keySnapshot.getKey());
        }

        @NonNull
        @Override
        public DatabaseReference onDisjoin(DataSnapshot keySnapshot) {
            return onJoin(keySnapshot); // Match the join/disjoin pair
        }

        @Override
        public void onJoinFailed(DataSnapshot snapshot, int index) {
            Log.w(TAG, "Key not found at ref " + snapshot.getRef() + " for index " + index);
        }
    }
}
