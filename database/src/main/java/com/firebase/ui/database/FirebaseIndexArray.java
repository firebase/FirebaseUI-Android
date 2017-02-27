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
    private Map<Query, ValueEventListener> mRefs = new HashMap<>();

    private FirebaseArray<String> mKeySnapshots;
    private List<DataSnapshot> mDataSnapshots = new ArrayList<>();

    private Map<String, T> mObjectCache = new HashMap<>();

    /**
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link RecyclerView}.
     */
    public FirebaseIndexArray(Query keyQuery, DatabaseReference dataRef) {
        this(keyQuery, dataRef, null);
    }

    public FirebaseIndexArray(Query keyQuery, DatabaseReference dataRef, SnapshotParser<T> parser) {
        super(parser);

        mKeySnapshots = new FirebaseArray<>(keyQuery, new SnapshotParser<String>() {
            @Override
            public String parseSnapshot(DataSnapshot snapshot) {
                return snapshot.getKey();
            }
        });

        mDataRef = dataRef;

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
        // No-op, we don't listen to batch events for the key ref
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
    }

    @Override
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        super.removeChangeEventListener(listener);
        if (!isListening()) {
            for (Query query : mRefs.keySet()) {
                query.removeEventListener(mRefs.get(query));
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
        String key = data.getKey();
        Query ref = mDataRef.child(key);

        // Start listening
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener()));
    }

    protected void onKeyMoved(DataSnapshot data, int index, int oldIndex) {
        String key = data.getKey();

        if (isKeyAtIndex(key, oldIndex)) {
            DataSnapshot snapshot = removeData(oldIndex);
            int newIndex = getIndexForKey(key);
            mDataSnapshots.add(newIndex, snapshot);
            notifyChangeEventListeners(ChangeEventListener.EventType.MOVED, snapshot, newIndex, oldIndex);
        }
    }

    protected void onKeyRemoved(DataSnapshot data, int index) {
        String key = data.getKey();
        mDataRef.child(key).removeEventListener(mRefs.remove(mDataRef.getRef().child(key)));

        if (isKeyAtIndex(key, index)) {
            DataSnapshot snapshot = removeData(index);
            notifyChangeEventListeners(ChangeEventListener.EventType.REMOVED, snapshot, index);
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
                if (!isKeyAtIndex(key, index)) {
                    // We don't already know about this data, add it
                    mDataSnapshots.add(index, snapshot);
                    notifyChangeEventListeners(ChangeEventListener.EventType.ADDED, snapshot, index);
                } else {
                    // We already know about this data, just update it
                    updateData(index, snapshot);
                    notifyChangeEventListeners(ChangeEventListener.EventType.CHANGED, snapshot, index);
                }
            } else {
                if (isKeyAtIndex(key, index)) {
                    // This data has disappeared, remove it
                    removeData(index);
                    notifyChangeEventListeners(ChangeEventListener.EventType.REMOVED, snapshot, index);
                } else {
                    // Data we never knew about has disappeared
                    Log.w(TAG, "Key not found at ref: " + snapshot.getRef());
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            notifyListenersOnCancelled(error);
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

}
