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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class FirebaseIndexArray extends FirebaseArray {
    private static final String TAG = "FirebaseIndexArray";
    private static final ChangeEventListener NOOP_CHANGE_LISTENER = new ChangeEventListener() {
        @Override
        public void onChange(EventType type, int index, int oldIndex) {
        }

        @Override
        public void onCancelled(DatabaseError error) {
        }
    };

    private ChangeEventListener mListenerCopy;
    private JoinResolver mJoinResolver;
    private Map<Query, ValueEventListener> mRefs = new HashMap<>();
    private List<DataSnapshot> mDataSnapshots = new ArrayList<>();

    public FirebaseIndexArray(Query keyRef) {
        super(keyRef);
    }

    @Override
    public void setChangeEventListener(@NonNull ChangeEventListener listener) {
        super.setChangeEventListener(listener);
        mListenerCopy = listener;
    }

    public void setJoinResolver(@NonNull JoinResolver joinResolver) {
        if (mIsListening && joinResolver == null) {
            throw new IllegalStateException("Join resolver cannot be null.");
        }
        mJoinResolver = joinResolver;
    }

    @Override
    public void startListening() {
        if (mJoinResolver == null) throw new IllegalStateException("Join resolver cannot be null.");
        super.startListening();
    }

    @Override
    public void stopListening() {
        super.stopListening();
        Set<Query> refs = new HashSet<>(mRefs.keySet());
        for (Query ref : refs) {
            ref.removeEventListener(mRefs.remove(ref));
        }
        mDataSnapshots.clear();
    }

    @Override
    public int size() {
        return mDataSnapshots.size();
    }

    @Override
    public DataSnapshot get(int index) {
        return mDataSnapshots.get(index);
    }

    @Override
    public void onChildAdded(DataSnapshot keySnapshot, String previousChildKey) {
        super.setChangeEventListener(NOOP_CHANGE_LISTENER);
        super.onChildAdded(keySnapshot, previousChildKey);
        super.setChangeEventListener(mListenerCopy);

        Query ref = mJoinResolver.onJoin(keySnapshot, previousChildKey);
        mRefs.put(ref, ref.addValueEventListener(new DataRefListener()));
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        super.setChangeEventListener(NOOP_CHANGE_LISTENER);
        super.onChildChanged(snapshot, previousChildKey);
        super.setChangeEventListener(mListenerCopy);
    }

    @Override
    public void onChildRemoved(DataSnapshot keySnapshot) {
        String key = keySnapshot.getKey();
        int index = getIndexForKey(key);

        Query removeQuery = mJoinResolver.onDisjoin(keySnapshot);
        removeQuery.removeEventListener(mRefs.remove(removeQuery));

        super.setChangeEventListener(NOOP_CHANGE_LISTENER);
        super.onChildRemoved(keySnapshot);
        super.setChangeEventListener(mListenerCopy);

        if (isMatch(index, key)) {
            mDataSnapshots.remove(index);
            notifyChangeListener(ChangeEventListener.EventType.REMOVED, index);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot keySnapshot, String previousChildKey) {
        String key = keySnapshot.getKey();
        int oldIndex = getIndexForKey(key);

        super.setChangeEventListener(NOOP_CHANGE_LISTENER);
        super.onChildMoved(keySnapshot, previousChildKey);
        super.setChangeEventListener(mListenerCopy);

        if (isMatch(oldIndex, key)) {
            DataSnapshot snapshot = mDataSnapshots.remove(oldIndex);
            int newIndex = getIndexForKey(key);
            mDataSnapshots.add(newIndex, snapshot);
            mListener.onChange(ChangeEventListener.EventType.MOVED, newIndex, oldIndex);
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.e(TAG, "A fatal error occurred retrieving the necessary keys to populate your adapter.");
        super.onCancelled(error);
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

    private class DataRefListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            String key = snapshot.getKey();
            int index = getIndexForKey(key);

            if (snapshot.getValue() != null) {
                if (!isMatch(index, key)) {
                    mDataSnapshots.add(index, snapshot);
                    notifyChangeListener(ChangeEventListener.EventType.ADDED, index);
                } else {
                    mDataSnapshots.set(index, snapshot);
                    notifyChangeListener(ChangeEventListener.EventType.CHANGED, index);
                }
            } else {
                if (isMatch(index, key)) {
                    mDataSnapshots.remove(index);
                    notifyChangeListener(ChangeEventListener.EventType.REMOVED, index);
                } else {
                    mJoinResolver.onJoinFailed(index, snapshot);
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            mListener.onCancelled(error);
        }
    }
}
