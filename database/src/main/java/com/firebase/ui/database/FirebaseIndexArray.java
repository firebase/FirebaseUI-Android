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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

class FirebaseIndexArray extends FirebaseArray implements ValueEventListener {
    private static final String TAG = FirebaseIndexArray.class.getSimpleName();

    private DatabaseReference mRef;
    private List<DataSnapshot> mDataSnapshots = new ArrayList<>();
    private OnChangedListener mListener;

    FirebaseIndexArray(Query keyRef, DatabaseReference dataRef) {
        super(keyRef);
        mRef = dataRef;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        for (DataSnapshot snapshot : mDataSnapshots) {
            snapshot.getRef().removeEventListener((ValueEventListener) this);
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

    private int getIndexForKey(String key) {
        int dataCount = getCount();
        int index = 0;
        for (int keyIndex = 0; index < dataCount; keyIndex++) {
            String superKey = super.getItem(keyIndex).getKey();
            if (key.equals(superKey)) {
                break;
            } else if (getItem(index).getKey().equals(superKey)) {
                index++;
            }
        }
        return index;
    }

    private boolean isMatch(int index, String key) {
        return index >= 0 && index < getCount() && getItem(index).getKey().equals(key);
    }

    @Override
    public void onChildAdded(DataSnapshot keySnapshot, String previousChildKey) {
        super.setOnChangedListener(null);
        super.onChildAdded(keySnapshot, previousChildKey);
        super.setOnChangedListener(mListener);
        mRef.child(keySnapshot.getKey()).addValueEventListener(this);
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        String key = snapshot.getKey();
        int index = getIndexForKey(key);

        if (snapshot.getValue() != null) {
            if (!isMatch(index, key)) {
                mDataSnapshots.add(index, snapshot);
                notifyChangedListeners(OnChangedListener.EventType.ADDED, index);
            } else {
                mDataSnapshots.set(index, snapshot);
                notifyChangedListeners(OnChangedListener.EventType.CHANGED, index);
            }
        } else {
            Log.w(TAG, "Key not found at ref: " + snapshot.getRef());
            if (isMatch(index, key)) {
                mDataSnapshots.remove(index);
                notifyChangedListeners(OnChangedListener.EventType.REMOVED, index);
            }
        }
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
        mRef.child(key).removeEventListener((ValueEventListener) this);

        super.setOnChangedListener(null);
        super.onChildRemoved(keySnapshot);
        super.setOnChangedListener(mListener);

        if (isMatch(index, key)) {
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

        if (isMatch(oldIndex, key)) {
            DataSnapshot snapshot = mDataSnapshots.remove(oldIndex);
            int newIndex = getIndexForKey(key);
            mDataSnapshots.add(newIndex, snapshot);
            notifyChangedListeners(OnChangedListener.EventType.MOVED, newIndex, oldIndex);
        }
    }

    @Override
    public void setOnChangedListener(OnChangedListener listener) {
        super.setOnChangedListener(listener);
        mListener = listener;
    }
}
