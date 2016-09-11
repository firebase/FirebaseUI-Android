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
    private OnChangedListener mListener;
    private List<DataSnapshot> mSnapshots = new ArrayList<>();
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

        throw new IllegalArgumentException("Key not found");
    }

    @Override
    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }

    @Override
    protected void notifyChangedListeners(OnChangedListener.EventType type, final int index, int oldIndex) {
        if (mListener != null) {
            switch (type) {
                case Added:
                    final boolean[] isNewListener = {true};

                    ValueEventListener valueEventListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                if (isNewListener[0]) {
                                    mSnapshots.add(index, dataSnapshot);
                                    mListener.onChanged(OnChangedListener.EventType.Added, index, -1);

                                    isNewListener[0] = false;
                                } else {
                                    mSnapshots.set(getIndexForKey(dataSnapshot.getKey()), dataSnapshot);
                                    mListener.onChanged(OnChangedListener.EventType.Changed,
                                                        getIndexForKey(dataSnapshot.getKey()),
                                                        -1);
                                }
                            } else {
                                Log.w("Firebase-UI", "Key not found at ref: " + dataSnapshot.getRef().toString());
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            notifyCancelledListeners(databaseError);
                        }
                    };

                    DatabaseReference ref = mRef.child(internalGetItem(index).getKey());
                    ref.addValueEventListener(valueEventListener);
                    mValueEventListeners.put(ref, valueEventListener);
                    break;
                case Changed:
                    // We don't care because the key's value shouldn't matter.
                    break;
                case Removed:
                    DatabaseReference rmRef = mRef.child(mSnapshots.get(index).getKey());
                    rmRef.removeEventListener(mValueEventListeners.get(rmRef));
                    mValueEventListeners.remove(rmRef);
                    mSnapshots.remove(index);

                    mListener.onChanged(OnChangedListener.EventType.Removed, index, -1);
                    break;
                case Moved:
                    DataSnapshot tmp = mSnapshots.remove(oldIndex);
                    mSnapshots.add(index, tmp);

                    mListener.onChanged(OnChangedListener.EventType.Moved, index, oldIndex);
                    break;
                default:
                    throw new IllegalStateException("Incomplete case statement");
            }
        }
    }
}
