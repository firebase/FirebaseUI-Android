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

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class FirebaseIndexArray extends FirebaseArray {
    private static final String TAG = FirebaseIndexArray.class.getSimpleName();

    private final Query mQuery;
    private final HashMap<Query, ValueEventListener> mRefs = new HashMap<>();
    private final ArrayList<DataSnapshot> mDataSnapshots = new ArrayList<>();
    private OnChangedListener mListener;

    public FirebaseIndexArray(Query keyRef, Query dataRef) {
        super(keyRef);
        mQuery = dataRef;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        final Iterator<Map.Entry<Query, ValueEventListener>> it = mRefs.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<Query, ValueEventListener> entry = it.next();
            entry.getKey().removeEventListener(entry.getValue());
            it.remove();
        }
    }

    @Override
    public int getCount() {
        return mDataSnapshots.size();
    }

    @Override
    public DataSnapshot getItem(int index) {
        return mDataSnapshots.get(index);
    }

    private boolean isKeyAtIndex(int index, String key) {
        return index >= 0 && index < getCount() && mDataSnapshots.get(index).getKey().equals(key);
    }

    @Override
    public void onChildAdded(DataSnapshot keySnapshot, String previousChildKey) {
        super.setOnChangedListener(null);
        super.onChildAdded(keySnapshot, previousChildKey);
        super.setOnChangedListener(mListener);

        Query ref = mQuery.getRef().child(keySnapshot.getKey());
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener()));
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        super.setOnChangedListener(null);
        super.onChildChanged(snapshot, previousChildKey);
        super.setOnChangedListener(mListener);
    }

    @Override
    public void onChildRemoved(DataSnapshot keySnapshot) {
        String key = keySnapshot.getKey();
        int index = getIndexForKey(key);
        mQuery.getRef().child(key).removeEventListener(mRefs.remove(mQuery.getRef().child(key)));

        super.setOnChangedListener(null);
        super.onChildRemoved(keySnapshot);
        super.setOnChangedListener(mListener);

        if (isKeyAtIndex(index, key)) {
            mDataSnapshots.remove(index);
            notifyChangedListeners(OnChangedListener.EventType.REMOVED, index);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot keySnapshot, String previousChildKey) {
        String key = keySnapshot.getKey();
        int oldIndex = getIndexForKey(key);

        super.setOnChangedListener(null);
        super.onChildMoved(keySnapshot, previousChildKey);
        super.setOnChangedListener(mListener);

        if (isKeyAtIndex(oldIndex, key)) {
            DataSnapshot snapshot = mDataSnapshots.remove(oldIndex);
            int newIndex = getIndexForKey(key);
            mDataSnapshots.add(newIndex, snapshot);
            notifyChangedListeners(OnChangedListener.EventType.MOVED, newIndex, oldIndex);
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
        super.onCancelled(error);
    }

    @Override
    public void setOnChangedListener(OnChangedListener listener) {
        super.setOnChangedListener(listener);
        mListener = listener;
    }

    private class DataRefListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            String key = snapshot.getKey();
            int index = getIndexForKey(key);

            if (snapshot.getValue() != null) {
                if (!isKeyAtIndex(index, key)) {
                    mDataSnapshots.add(index, snapshot);
                    notifyChangedListeners(OnChangedListener.EventType.ADDED, index);
                } else {
                    mDataSnapshots.set(index, snapshot);
                    notifyChangedListeners(OnChangedListener.EventType.CHANGED, index);
                }
            } else {
                if (isKeyAtIndex(index, key)) {
                    mDataSnapshots.remove(index);
                    notifyChangedListeners(OnChangedListener.EventType.REMOVED, index);
                } else {
                    Log.w(TAG, "Key not found at ref: " + snapshot.getRef());
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            notifyCancelledListeners(error);
        }
    }
}
