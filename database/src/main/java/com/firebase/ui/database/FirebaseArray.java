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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * This class implements an array-like collection on top of a Firebase location.
 */
class FirebaseArray implements ChildEventListener, ValueEventListener {
    public interface OnChangedListener {
        enum EventType {ADDED, CHANGED, REMOVED, MOVED}

        void onChildChanged(EventType type, int index, int oldIndex);

        void onDataChanged();

        void onCancelled(DatabaseError databaseError);

    }

    private final Query mQuery;
    private final ArrayList<DataSnapshot> mSnapshots = new ArrayList<>();
    private final HashMap<String, Integer> mSnapshotMap = new HashMap<>();
    private OnChangedListener mListener;

    FirebaseArray(Query ref) {
        mQuery = ref;
        mQuery.addChildEventListener(this);
        mQuery.addValueEventListener(this);
    }

    public void cleanup() {
        mQuery.removeEventListener((ValueEventListener) this);
        mQuery.removeEventListener((ChildEventListener) this);
    }

    public int getCount() {
        return mSnapshots.size();
    }

    public DataSnapshot getItem(int index) {
        return mSnapshots.get(index);
    }

    public int getIndexForKey(String key) {
        final Integer index = mSnapshotMap.get(key);
        if (index != null) {
            return index;
        } else {
            throw new IllegalArgumentException("Key not found");
        }
    }

    private int scanArrayForKey(String key) {
        final ListIterator<DataSnapshot> it = mSnapshots.listIterator();
        while (it.hasNext()) {
            if (it.next().getKey().equals(key)) {
                return it.previousIndex();
            }
        }
        throw new IllegalArgumentException("Key not found");
    }

    @Override
    public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
        int index = 0;
        if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }
        mSnapshots.add(index, snapshot);
        mSnapshotMap.put(snapshot.getKey(), index);
        notifyChangedListeners(OnChangedListener.EventType.ADDED, index);
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.set(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.CHANGED, index);
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(index);
        mSnapshotMap.remove(snapshot.getKey());
        notifyChangedListeners(OnChangedListener.EventType.REMOVED, index);
    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : (scanArrayForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);

        // Rebuild the map of indices
        mSnapshotMap.clear();
        for (int i = 0; i < mSnapshots.size(); i++) {
            mSnapshotMap.put(mSnapshots.get(i).getKey(), i);
        }

        notifyChangedListeners(OnChangedListener.EventType.MOVED, newIndex, oldIndex);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        mListener.onDataChanged();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        notifyCancelledListeners(error);
    }

    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }

    void notifyChangedListeners(OnChangedListener.EventType type, int index) {
        notifyChangedListeners(type, index, -1);
    }

    void notifyChangedListeners(OnChangedListener.EventType type, int index, int oldIndex) {
        if (mListener != null) {
            mListener.onChildChanged(type, index, oldIndex);
        }
    }

    void notifyCancelledListeners(DatabaseError databaseError) {
        if (mListener != null) {
            mListener.onCancelled(databaseError);
        }
    }
}
