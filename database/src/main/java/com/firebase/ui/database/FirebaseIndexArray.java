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
    private List<DataSnapshot> mDataSnapshots = new ArrayList<>();

    /**
     * When keys are added in {@link FirebaseArray}, we need to fetch the data async. This list
     * contains keys that exist in the backing {@link FirebaseArray}, but their data hasn't been
     * downloaded yet in this array.
     */
    private List<String> mKeysWithPendingUpdate = new ArrayList<>();
    /**
     * Moves or deletions don't need to fetch new data so they can be performed instantly once the
     * backing {@link FirebaseArray} is done updating. This will be true if the backing {@link
     * FirebaseArray} is in the middle of an update, false otherwise.
     */
    private boolean mHasPendingMoveOrDelete;
    /**
     * Sigh, more null value bugs. We need to get {@code onChildMoved}'s {@code oldIndex} from our
     * list of indices instead of the key snapshots or we might get tripped up by null values.
     * <p>
     * This field stores our version of {@code oldIndex} and the expected contract is for its value
     * to be used immediately after the key snapshots update themselves and send the {@code
     * onChildMoved} event.
     */
    private int mPendingMoveIndex;

    /**
     * Create a new FirebaseIndexArray that parses snapshots as members of a given class.
     *
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link RecyclerView}.
     * @see ObservableSnapshotArray#ObservableSnapshotArray(Class)
     */
    public FirebaseIndexArray(Query keyQuery, DatabaseReference dataRef, Class<T> tClass) {
        super(tClass);
        init(keyQuery, dataRef);
    }

    /**
     * Create a new FirebaseIndexArray with a custom {@link SnapshotParser}.
     *
     * @see ObservableSnapshotArray#ObservableSnapshotArray(SnapshotParser)
     * @see FirebaseIndexArray#FirebaseIndexArray(Query, DatabaseReference, Class)
     */
    public FirebaseIndexArray(Query keyQuery, DatabaseReference dataRef, SnapshotParser<T> parser) {
        super(parser);
        init(keyQuery, dataRef);
    }

    private void init(Query keyQuery, DatabaseReference dataRef) {
        mDataRef = dataRef;
        mKeySnapshots = new FirebaseArray<>(keyQuery, new SnapshotParser<String>() {
            @Override
            public String parseSnapshot(DataSnapshot snapshot) {
                return snapshot.getKey();
            }
        });

        mKeySnapshots.setPreChangeEventListener(new FirebaseArray.PreChangeEventListener() {
            @Override
            public void onPreMove(DataSnapshot data, int oldIndex) {
                mPendingMoveIndex = returnOrFindIndexForKey(oldIndex, data.getKey());
            }
        });
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mKeySnapshots.addChangeEventListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mKeySnapshots.removeChangeEventListener(this);

        for (DatabaseReference ref : mRefs.keySet()) {
            ref.removeEventListener(mRefs.get(ref));
        }
        mRefs.clear();
    }

    @Override
    public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
        switch (type) {
            case ADDED:
                onKeyAdded(snapshot, index);
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
        if (mHasPendingMoveOrDelete || mKeySnapshots.isEmpty()) {
            notifyListenersOnDataChanged();
            mHasPendingMoveOrDelete = false;
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
    }

    @Override
    protected List<DataSnapshot> getSnapshots() {
        return mDataSnapshots;
    }

    private int returnOrFindIndexForKey(int index, String key) {
        int realIndex;
        if (isKeyAtIndex(key, index)) {
            realIndex = index;
        } else {
            int dataCount = size();
            int dataIndex = 0;
            for (int keyIndex = 0; dataIndex < dataCount; keyIndex++) {
                if (keyIndex == mKeySnapshots.size()) { break; }

                String superKey = mKeySnapshots.getObject(keyIndex);
                if (key.equals(superKey)) {
                    break;
                } else if (mDataSnapshots.get(dataIndex).getKey().equals(superKey)) {
                    dataIndex++;
                }
            }
            realIndex = dataIndex;
        }
        return realIndex;
    }

    /**
     * Determines if a DataSnapshot with the given key is present at the given index.
     */
    private boolean isKeyAtIndex(String key, int index) {
        return index >= 0 && index < size() && mDataSnapshots.get(index).getKey().equals(key);
    }

    /**
     * @deprecated
     */
    @Deprecated
    protected void onKeyAdded(DataSnapshot data) {}

    protected void onKeyAdded(DataSnapshot data, int index) {
        onKeyAdded(data);

        String key = data.getKey();
        DatabaseReference ref = mDataRef.child(key);

        mKeysWithPendingUpdate.add(key);
        // Start listening
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener(index)));
    }

    protected void onKeyMoved(DataSnapshot data, int index, int oldIndex) {
        String key = data.getKey();

        int realOldIndex = mPendingMoveIndex;
        if (isKeyAtIndex(key, realOldIndex)) {
            DataSnapshot snapshot = removeData(realOldIndex);
            int realIndex = returnOrFindIndexForKey(index, key);
            mHasPendingMoveOrDelete = true;
            mDataSnapshots.add(realIndex, snapshot);
            notifyChangeEventListeners(EventType.MOVED, snapshot, realIndex, realOldIndex);
        }
    }

    protected void onKeyRemoved(DataSnapshot data, int index) {
        String key = data.getKey();
        ValueEventListener listener = mRefs.remove(mDataRef.getRef().child(key));
        if (listener != null) mDataRef.child(key).removeEventListener(listener);

        int realIndex = returnOrFindIndexForKey(index, key);
        if (isKeyAtIndex(key, realIndex)) {
            DataSnapshot snapshot = removeData(realIndex);
            mHasPendingMoveOrDelete = true;
            notifyChangeEventListeners(EventType.REMOVED, snapshot, realIndex);
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
        private int currentIndex;

        /**
         * Use {@link #DataRefListener(int)} instead and provide the starting index.
         */
        @Deprecated
        public DataRefListener() {}

        public DataRefListener(int index) {
            currentIndex = index;
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            String key = snapshot.getKey();
            int index = currentIndex = returnOrFindIndexForKey(currentIndex, key);

            if (snapshot.getValue() != null) {
                if (isKeyAtIndex(key, index)) {
                    // We already know about this data, just update it
                    updateData(index, snapshot);
                    notifyChangeEventListeners(EventType.CHANGED, snapshot, index);
                } else {
                    // We don't already know about this data, add it
                    mDataSnapshots.add(index, snapshot);
                    notifyChangeEventListeners(EventType.ADDED, snapshot, index);
                }
            } else {
                if (isKeyAtIndex(key, index)) {
                    // This data has disappeared, remove it
                    removeData(index);
                    notifyChangeEventListeners(EventType.REMOVED, snapshot, index);
                } else {
                    // Data does not exist
                    Log.w(TAG, "Key not found at ref: " + snapshot.getRef());
                }
            }

            // In theory, we would only want to pop the queue if this listener was just added
            // i.e. `snapshot.value != null && isKeyAtIndex(...)`. However, if the developer makes a
            // mistake and `snapshot.value == null`, we will never pop the queue and
            // `notifyListenersOnDataChanged()` will never be called. Thus, we pop the queue anytime
            // an update is received.
            mKeysWithPendingUpdate.remove(key);
            if (mKeysWithPendingUpdate.isEmpty()) notifyListenersOnDataChanged();
        }

        @Override
        public void onCancelled(DatabaseError error) {
            notifyListenersOnCancelled(error);
        }
    }
}
