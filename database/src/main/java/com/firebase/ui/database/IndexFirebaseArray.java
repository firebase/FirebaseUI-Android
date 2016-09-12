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

import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

class IndexFirebaseArray extends FirebaseArray {
    private DatabaseReference mRef;
    private List<DataSnapshot> mSnapshots = new ArrayList<>();
    private List<DataSnapshot> mNullableSnapshots = new ArrayList<>();
    private SimpleArrayMap<DatabaseReference, ValueEventListener> mValueEventListeners = new SimpleArrayMap<>();

    public IndexFirebaseArray(Query keyRef, DatabaseReference dataRef) {
        super(keyRef);
        mRef = dataRef;
    }

    @Override
    public void cleanup() {
        super.cleanup();

        for (int i = 0; i < mValueEventListeners.size(); i++) {
            DatabaseReference ref = mValueEventListeners.keyAt(i);
            ref.removeEventListener(mValueEventListeners.get(ref));
        }
    }

    @Override
    public int getCount() {
        return mSnapshots.size();
    }

    @Override
    public DataSnapshot getItem(int index) {
        return mSnapshots.get(index);
    }

    private int getIndexForKey(String key) {
        int index = 0;
        for (DataSnapshot snapshot : mSnapshots) {
            if (snapshot.getKey().equals(key)) {
                return index;
            } else {
                index++;
            }
        }

        // Searching through non-null snapshots failed. We now need to look through the nullable snapshots.

        return getNullableSnapshotIndex(key) - getNullsBeforeInclusiveIndex(index);
    }

    private int getNullableSnapshotIndex(String key) {
        int index = 0;
        for (DataSnapshot snapshot : mNullableSnapshots) {
            if (snapshot.getKey().equals(key)) {
                return index;
            } else {
                index++;
            }
        }
        throw new IllegalArgumentException("Key not found");
    }

    private int getNullsBeforeInclusiveIndex(int index) {
        int total = 0;
        for (int i = 0; i <= index; i++) {
            if (mNullableSnapshots.get(i).getValue() == null) {
                total++;
            }
        }
        return total;
    }

    private boolean containsKey(String key) {
        for (DataSnapshot snapshot : mNullableSnapshots) {
            if (snapshot.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onChildAdded(DataSnapshot keySnapshot, final String previousChildKey) {
        final boolean[] isNewListener = {true};

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    if (isNewListener[0]) {
                        int index = 0;
                        if (previousChildKey != null) {
                            index = getIndexForKey(previousChildKey) + 1;
                        }
                        add(index, snapshot);
                        notifyChangedListeners(OnChangedListener.EventType.Added, index);

                        isNewListener[0] = false;
                    } else {
                        int index = getIndexForKey(snapshot.getKey());
                        set(index, snapshot);
                        notifyChangedListeners(OnChangedListener.EventType.Changed, index);
                    }
                } else {
                    Log.w("Firebase-UI", "Key not found at ref: " + snapshot.getRef().toString());

                    if (!containsKey(snapshot.getKey())) {
                        int index = 0;
                        if (previousChildKey != null) {
                            index = getIndexForKey(previousChildKey) + 1;
                        }
                        mNullableSnapshots.add(index, snapshot);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                notifyCancelledListeners(databaseError);
            }
        };

        DatabaseReference ref = mRef.child(keySnapshot.getKey());
        ref.addValueEventListener(valueEventListener);
        mValueEventListeners.put(ref, valueEventListener);
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        DatabaseReference ref = mRef.child(snapshot.getKey());

        ref.removeEventListener(mValueEventListeners.get(ref));
        remove(index);
        mValueEventListeners.remove(ref);

        notifyChangedListeners(OnChangedListener.EventType.Removed, index);
    }

    @Override
    public void onChildMoved(DataSnapshot keySnapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(keySnapshot.getKey());
        DataSnapshot snapshot = remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : getIndexForKey(previousChildKey) + 1;
        add(newIndex, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Moved, newIndex, oldIndex);
    }

    private void add(int index, DataSnapshot snapshot) {
        mSnapshots.add(index, snapshot);
        mNullableSnapshots.add(index, snapshot);
    }

    private DataSnapshot set(int index, DataSnapshot snapshot) {
        mNullableSnapshots.set(index, snapshot);
        return mSnapshots.set(index, snapshot);
    }

    private DataSnapshot remove(int index) {
        mNullableSnapshots.remove(index);
        return mSnapshots.remove(index);
    }
}
