package com.firebase.ui.database;

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

import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

class FirebaseIndexArray extends FirebaseArray implements ValueEventListener {

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
        for (DataSnapshot dataSnapshot : mDataSnapshots) {
            dataSnapshot.getRef().removeEventListener((ValueEventListener)this);
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
        final int keysCount = super.getCount();
        final int dataCount = getCount();
        int keyIndex = 0;
        int dataIndex = 0;
        while (dataIndex < dataCount && keyIndex < keysCount) {
            DataSnapshot keySnapshot = super.getItem(keyIndex);
            if (keySnapshot.getKey().equals(key)) {
                break;
            } else if (getItem(dataIndex).getKey().equals(keySnapshot.getKey())) {
                ++dataIndex;
            }
            ++keyIndex;
        }
        return dataIndex;
    }

    @Override
    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
        super.setOnChangedListener(listener);
    }

    @Override
    public void onChildAdded(DataSnapshot keySnapshot, String previousChildKey) {
        super.setOnChangedListener(null);
        super.onChildAdded(keySnapshot, previousChildKey);
        super.setOnChangedListener(mListener);
        mRef.child(keySnapshot.getKey()).addValueEventListener(this);
    }

    @Override
    public void onChildChanged(DataSnapshot keySnapshot, String previousChildKey) {
        super.setOnChangedListener(null);
        super.onChildChanged(keySnapshot, previousChildKey);
        super.setOnChangedListener(mListener);
    }

    @Override
    public void onChildRemoved(DataSnapshot keySnapshot) {
        super.setOnChangedListener(null);
        super.onChildRemoved(keySnapshot);
        super.setOnChangedListener(mListener);

        String key = keySnapshot.getKey();
        mRef.child(key).removeEventListener((ValueEventListener)this);

        int index = getIndexForKey(key);
        if (doesItemAtIndexHaveKey(index, key)) {
            mDataSnapshots.remove(index);
            notifyChangedListeners(OnChangedListener.EventType.Removed, index);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot keySnapshot, String previousChildKey) {
        String key = keySnapshot.getKey();
        int oldIndex = getIndexForKey(key);

        super.setOnChangedListener(null);
        super.onChildMoved(keySnapshot, previousChildKey);
        super.setOnChangedListener(mListener);

        if (doesItemAtIndexHaveKey(oldIndex, key)) {
            DataSnapshot dataSnapshot = mDataSnapshots.remove(oldIndex);
            int newIndex = getIndexForKey(key);
            mDataSnapshots.add(newIndex, dataSnapshot);
            notifyChangedListeners(OnChangedListener.EventType.Moved, newIndex, oldIndex);
        }
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        String key = dataSnapshot.getKey();
        int index = getIndexForKey(key);
        boolean hasAnyValue = dataSnapshot.getValue() != null;
        if (doesItemAtIndexHaveKey(index, key)) {
            if (hasAnyValue) {
                mDataSnapshots.set(index, dataSnapshot);
                notifyChangedListeners(OnChangedListener.EventType.Changed, index);
            } else {
                mDataSnapshots.remove(index);
                notifyChangedListeners(OnChangedListener.EventType.Removed, index);
            }
        } else if (hasAnyValue){
            mDataSnapshots.add(index, dataSnapshot);
            notifyChangedListeners(OnChangedListener.EventType.Added, index);
        }
    }

    private boolean doesItemAtIndexHaveKey(int index, @NonNull String key) {
        if (index < 0 || index >= getCount()) {
            return false;
        }
        return getItem(index).getKey().equals(key);
    }
}
