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
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class FirebaseIndexArray extends FirebaseArray {
    private static final String TAG = "FirebaseIndexArray";

    private Query mDataQuery;
    private Map<Query, ValueEventListener> mRefs = new HashMap<>();
    private List<DataSnapshot> mDataSnapshots = new ArrayList<>();

    public FirebaseIndexArray(Query keyQuery, Query dataQuery) {
        super(keyQuery);
        mDataQuery = dataQuery;
    }

    @Override
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        super.removeChangeEventListener(listener);
        if (!isListening()) {
            Set<Query> refs = new HashSet<>(mRefs.keySet());
            for (Query ref : refs) {
                ref.removeEventListener(mRefs.remove(ref));
            }
            mDataSnapshots.clear();
        }
    }

    private int getIndexForKey(String key) {
        int dataCount = size();
        int index = 0;
        for (int keyIndex = 0; index < dataCount; keyIndex++) {
            String superKey = super.get(keyIndex).getKey();
            if (key.equals(superKey)) {
                break;
            } else if (mDataSnapshots.get(index).getKey().equals(superKey)) {
                index++;
            }
        }
        return index;
    }

    private boolean isMatch(int index, String key) {
        return index >= 0 && index < size() && mDataSnapshots.get(index).getKey().equals(key);
    }

    @Override
    public void onChildAdded(DataSnapshot keySnapshot, String previousChildKey) {
        setShouldNotifyListeners(false);
        super.onChildAdded(keySnapshot, previousChildKey);
        setShouldNotifyListeners(true);

        Query ref = mDataQuery.getRef().child(keySnapshot.getKey());
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener()));
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        setShouldNotifyListeners(false);
        super.onChildChanged(snapshot, previousChildKey);
        setShouldNotifyListeners(true);
    }

    @Override
    public void onChildRemoved(DataSnapshot keySnapshot) {
        String key = keySnapshot.getKey();
        int index = getIndexForKey(key);
        mDataQuery.getRef()
                .child(key)
                .removeEventListener(mRefs.remove(mDataQuery.getRef().child(key)));

        setShouldNotifyListeners(false);
        super.onChildRemoved(keySnapshot);
        setShouldNotifyListeners(true);

        if (isMatch(index, key)) {
            mDataSnapshots.remove(index);
            notifyChangeEventListeners(ChangeEventListener.EventType.REMOVED, index);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot keySnapshot, String previousChildKey) {
        String key = keySnapshot.getKey();
        int oldIndex = getIndexForKey(key);

        setShouldNotifyListeners(false);
        super.onChildMoved(keySnapshot, previousChildKey);
        setShouldNotifyListeners(true);

        if (isMatch(oldIndex, key)) {
            DataSnapshot snapshot = mDataSnapshots.remove(oldIndex);
            int newIndex = getIndexForKey(key);
            mDataSnapshots.add(newIndex, snapshot);
            notifyChangeEventListeners(ChangeEventListener.EventType.MOVED, newIndex, oldIndex);
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
        super.onCancelled(error);
    }

    private class DataRefListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            String key = snapshot.getKey();
            int index = getIndexForKey(key);

            if (snapshot.getValue() != null) {
                if (!isMatch(index, key)) {
                    mDataSnapshots.add(index, snapshot);
                    notifyChangeEventListeners(ChangeEventListener.EventType.ADDED, index);
                } else {
                    mDataSnapshots.set(index, snapshot);
                    notifyChangeEventListeners(ChangeEventListener.EventType.CHANGED, index);
                }
            } else {
                if (isMatch(index, key)) {
                    mDataSnapshots.remove(index);
                    notifyChangeEventListeners(ChangeEventListener.EventType.REMOVED, index);
                } else {
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
    public int size() {
        return mDataSnapshots.size();
    }

    @Override
    public boolean isEmpty() {
        return mDataSnapshots.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return mDataSnapshots.contains(o);
    }

    @Override
    public Iterator<DataSnapshot> iterator() {
        return new ImmutableIterator(mDataSnapshots.iterator());
    }

    @Override
    public Object[] toArray() {
        return mDataSnapshots.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return mDataSnapshots.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return mDataSnapshots.containsAll(c);
    }

    @Override
    public DataSnapshot get(int index) {
        return mDataSnapshots.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return mDataSnapshots.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return mDataSnapshots.lastIndexOf(o);
    }

    @Override
    public ListIterator<DataSnapshot> listIterator() {
        return new ImmutableListIterator(mDataSnapshots.listIterator());
    }

    @Override
    public ListIterator<DataSnapshot> listIterator(int index) {
        return new ImmutableListIterator(mDataSnapshots.listIterator(index));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;

        FirebaseIndexArray array = (FirebaseIndexArray) obj;

        return mDataQuery.equals(array.mDataQuery)
                && mRefs.equals(array.mRefs)
                && mDataSnapshots.equals(array.mDataSnapshots);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mDataQuery.hashCode();
        result = 31 * result + mRefs.hashCode();
        result = 31 * result + mDataSnapshots.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FirebaseIndexArray{" +
                "mIsListening=" + isListening() +
                ", mDataQuery=" + mDataQuery +
                ", mDataSnapshots=" + mDataSnapshots +
                '}';
    }
}
