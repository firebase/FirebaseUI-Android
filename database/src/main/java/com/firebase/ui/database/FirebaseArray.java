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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * This class implements an array-like collection on top of a Firebase location.
 */
class FirebaseArray implements ChildEventListener {
    public interface OnChangedListener {
        enum EventType {Added, Changed, Removed, Moved}

        void onChanged(OnChangedListener.EventType type, int index, int oldIndex);
        
        void onCancelled(DatabaseError databaseError);
    }

    private Query mQuery;
    private DatabaseReference mRef;
    private OnChangedListener mListener;
    private ArrayList<DataSnapshot> mSnapshots = new ArrayList<>();
    private SimpleArrayMap<DatabaseReference, ValueEventListener> mValueEventListeners;

    public FirebaseArray(Query ref) {
        mQuery = ref;
        mQuery.addChildEventListener(this);
    }

    public FirebaseArray(Query keyRef, DatabaseReference dataRef) {
        mQuery = keyRef;
        mRef = dataRef;
        mQuery.addChildEventListener(this);
        mValueEventListeners = new SimpleArrayMap<>();
    }

    public void cleanup() {
        mQuery.removeEventListener(this);

        if (mRef != null) {
            for (int i = 0; i < mValueEventListeners.size(); i++) {
                DatabaseReference ref = mValueEventListeners.keyAt(i);
                ref.removeEventListener(mValueEventListeners.get(ref));
            }
        }
    }

    public int getCount() {
        return mSnapshots.size();
    }

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

    // Start of ChildEventListener methods
    @Override
    public void onChildAdded(DataSnapshot snapshot, final String previousChildKey) {
        if (mRef == null) {
            // In this case snapshot is the non-indexed data
            addChild(snapshot, previousChildKey);
        } else {
            // In this case snapshot is the key to the indexed data
            final boolean[] isNewListener = {true};

            ValueEventListener valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        if (isNewListener[0]) {
                            addChild(dataSnapshot, previousChildKey);
                            isNewListener[0] = false;
                        } else {
                            int index = getIndexForKey(dataSnapshot.getKey());
                            mSnapshots.set(index, dataSnapshot);
                            notifyChangedListeners(OnChangedListener.EventType.Changed, index);
                        }
                    } else {
                        throw new IllegalStateException("Key not found at ref: " + dataSnapshot.getRef().toString());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    notifyCancelledListeners(databaseError);
                }
            };

            DatabaseReference ref = mRef.child(snapshot.getKey());
            ref.addValueEventListener(valueEventListener);
            mValueEventListeners.put(ref, valueEventListener);
        }
    }

    private void addChild(DataSnapshot snapshot, String previousChildKey) {
        int index = 0;
        if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }
        mSnapshots.add(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Added, index);
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        if (mRef == null) {
            int index = getIndexForKey(snapshot.getKey());
            mSnapshots.set(index, snapshot);
            notifyChangedListeners(OnChangedListener.EventType.Changed, index);
        }
        // else: we don't care because the key's value should not change
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(index);
        notifyChangedListeners(OnChangedListener.EventType.Removed, index);

        if (mRef != null) {
            // We need to do some extra cleanup and remove the listener
            DatabaseReference ref = mRef.child(snapshot.getKey());
            ref.removeEventListener(mValueEventListeners.get(ref));
            mValueEventListeners.remove(ref);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, final String previousChildKey) {
        if (mRef == null) {
            moveChild(snapshot, previousChildKey);
        } else {
            mRef.child(snapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    moveChild(dataSnapshot, previousChildKey);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    notifyCancelledListeners(databaseError);
                }
            });
        }

    }

    private void moveChild(DataSnapshot snapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Moved, newIndex, oldIndex);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        notifyCancelledListeners(databaseError);
    }
    // End of ChildEventListener methods

    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }

    protected void notifyChangedListeners(OnChangedListener.EventType type, int index) {
        notifyChangedListeners(type, index, -1);
    }

    protected void notifyChangedListeners(OnChangedListener.EventType type, int index, int oldIndex) {
        if (mListener != null) {
            mListener.onChanged(type, index, oldIndex);
        }
    }

    protected void notifyCancelledListeners(DatabaseError databaseError) {
        if (mListener != null) {
            mListener.onCancelled(databaseError);
        }
    }
}
