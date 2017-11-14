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

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseIndexArray<T> extends ObservableSnapshotArray<T>
        implements ChangeEventListener {
    private static final String TAG = "FirebaseIndexArray";

    private final DatabaseReference mDataRef;
    private final Map<DatabaseReference, ValueEventListener> mRefs = new HashMap<>();

    private final FirebaseArray<String> mKeySnapshots;
    private final List<DataSnapshot> mDataSnapshots = new ArrayList<>();

    /**
     * When keys are added in {@link FirebaseArray}, we need to fetch the data async. This list
     * contains keys that exist in the backing {@link FirebaseArray}, but their data hasn't been
     * downloaded yet in this array.
     */
    private final List<String> mKeysWithPendingUpdate = new ArrayList<>();
    /**
     * Moves or deletions don't need to fetch new data so they can be performed instantly once the
     * backing {@link FirebaseArray} is done updating. This will be true if the backing {@link
     * FirebaseArray} is in the middle of an update, false otherwise.
     */
    private boolean mHasPendingMoveOrDelete;

    /**
     * Create a new FirebaseIndexArray with a custom {@link SnapshotParser}.
     *
     * @param keyQuery The Firebase location containing the list of keys to be found in {@code
     *                 dataRef}. Can also be a slice of a location, using some combination of {@code
     *                 limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef  The Firebase location to watch for data changes. Each key key found at {@code
     *                 keyQuery}'s location represents a list item in the {@link RecyclerView}.
     * @see ObservableSnapshotArray#ObservableSnapshotArray(SnapshotParser)
     */
    public FirebaseIndexArray(@NonNull Query keyQuery,
                              @NonNull DatabaseReference dataRef,
                              @NonNull SnapshotParser<T> parser) {
        super(parser);
        mDataRef = dataRef;
        mKeySnapshots = new FirebaseArray<>(keyQuery, new SnapshotParser<String>() {
            @NonNull
            @Override
            public String parseSnapshot(@NonNull DataSnapshot snapshot) {
                return snapshot.getKey();
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
    public void onChildChanged(@NonNull ChangeEventType type,
                               @NonNull DataSnapshot snapshot,
                               int newIndex,
                               int oldIndex) {
        switch (type) {
            case ADDED:
                onKeyAdded(snapshot, newIndex);
                break;
            case MOVED:
                onKeyMoved(snapshot, newIndex, oldIndex);
                break;
            case CHANGED:
                // This is a no-op, we don't care when a key 'changes' since that should not
                // be a supported operation
                break;
            case REMOVED:
                onKeyRemoved(snapshot, newIndex);
                break;
        }
    }

    @Override
    public void onDataChanged() {
        if (mHasPendingMoveOrDelete || mKeySnapshots.isEmpty()) {
            notifyOnDataChanged();
            mHasPendingMoveOrDelete = false;
        }
    }

    @Override
    public void onError(@NonNull DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
    }

    @NonNull
    @Override
    protected List<DataSnapshot> getSnapshots() {
        return mDataSnapshots;
    }

    private int returnOrFindIndexForKey(int index, String key) {
        int realIndex;
        if (isKeyAtIndex(key, index)) {
            // To optimize this query, if the expected item position is accurate, we simply return
            // it instead of searching for it in our keys all over again. This ensures developers
            // correctly indexing their data (i.e. no null values) don't take a performance hit.
            realIndex = index;
        } else {
            int dataCount = size();
            int dataIndex = 0;
            int keyIndex = 0;

            while (dataIndex < dataCount && keyIndex < mKeySnapshots.size()) {
                String superKey = mKeySnapshots.get(keyIndex);
                if (key.equals(superKey)) {
                    break;
                } else if (mDataSnapshots.get(dataIndex).getKey().equals(superKey)) {
                    // Only increment the data index if we aren't passing over a null value snapshot.
                    dataIndex++;
                }
                keyIndex++;
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

    private void onKeyAdded(DataSnapshot data, int newIndex) {
        String key = data.getKey();
        DatabaseReference ref = mDataRef.child(key);

        mKeysWithPendingUpdate.add(key);
        // Start listening
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener(newIndex)));
    }

    private void onKeyMoved(DataSnapshot data, int index, int oldIndex) {
        String key = data.getKey();

        // We can't use `returnOrFindIndexForKey(...)` for `oldIndex` or it might find the updated
        // index instead of the old one. Unfortunately, this does mean move events will be
        // incorrectly ignored if our list is a subset of the key list e.g. a key has null data.
        if (isKeyAtIndex(key, oldIndex)) {
            DataSnapshot snapshot = mDataSnapshots.remove(oldIndex);
            int realIndex = returnOrFindIndexForKey(index, key);
            mHasPendingMoveOrDelete = true;

            mDataSnapshots.add(realIndex, snapshot);
            notifyOnChildChanged(ChangeEventType.MOVED, snapshot, realIndex, oldIndex);
        }
    }

    private void onKeyRemoved(DataSnapshot data, int index) {
        String key = data.getKey();
        ValueEventListener listener = mRefs.remove(mDataRef.getRef().child(key));
        if (listener != null) mDataRef.child(key).removeEventListener(listener);

        int realIndex = returnOrFindIndexForKey(index, key);
        if (isKeyAtIndex(key, realIndex)) {
            DataSnapshot snapshot = mDataSnapshots.remove(realIndex);
            mHasPendingMoveOrDelete = true;
            notifyOnChildChanged(ChangeEventType.REMOVED, snapshot, realIndex, -1);
        }
    }

    /**
     * A ValueEventListener attached to the joined child data.
     */
    private final class DataRefListener implements ValueEventListener {
        /** Cached index to skip searching for the current index on each update */
        private int currentIndex;

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
                    mDataSnapshots.set(index, snapshot);
                    notifyOnChildChanged(ChangeEventType.CHANGED, snapshot, index, -1);
                } else {
                    // We don't already know about this data, add it
                    mDataSnapshots.add(index, snapshot);
                    notifyOnChildChanged(ChangeEventType.ADDED, snapshot, index, -1);
                }
            } else {
                if (isKeyAtIndex(key, index)) {
                    // This data has disappeared, remove it
                    mDataSnapshots.remove(index);
                    notifyOnChildChanged(ChangeEventType.REMOVED, snapshot, index, -1);
                } else {
                    // Data does not exist
                    Log.w(TAG, "Key not found at ref: " + snapshot.getRef());
                }
            }

            // In theory, we would only want to pop the queue if this listener was just added
            // i.e. `snapshot.value != null && isKeyAtIndex(...)`. However, if the developer makes a
            // mistake and `snapshot.value == null`, we will never pop the queue and
            // `notifyOnDataChanged()` will never be called. Thus, we pop the queue anytime
            // an update is received.
            mKeysWithPendingUpdate.remove(key);
            if (mKeysWithPendingUpdate.isEmpty()) notifyOnDataChanged();
        }

        @Override
        public void onCancelled(DatabaseError error) {
            notifyOnError(error);
        }
    }
}
