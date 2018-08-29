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
import android.support.annotation.Nullable;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a collection on top of a Firebase location.
 */
public class FirebaseArray<T> extends ObservableSnapshotArray<T>
        implements ChildEventListener, ValueEventListener {
    private final Query mQuery;
    private final List<DataSnapshot> mSnapshots = new ArrayList<>();

    /**
     * Create a new FirebaseArray with a custom {@link SnapshotParser}.
     *
     * @param query The Firebase location to watch for data changes. Can also be a slice of a
     *              location, using some combination of {@code limit()}, {@code startAt()}, and
     *              {@code endAt()}.
     * @see ObservableSnapshotArray#ObservableSnapshotArray(SnapshotParser)
     */
    public FirebaseArray(@NonNull Query query, @NonNull SnapshotParser<T> parser) {
        super(parser);
        mQuery = query;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mQuery.addChildEventListener(this);
        mQuery.addValueEventListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mQuery.removeEventListener((ValueEventListener) this);
        mQuery.removeEventListener((ChildEventListener) this);
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildKey) {
        int index = 0;
        if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }

        mSnapshots.add(index, snapshot);
        notifyOnChildChanged(ChangeEventType.ADDED, snapshot, index, -1);
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());

        mSnapshots.set(index, snapshot);
        notifyOnChildChanged(ChangeEventType.CHANGED, snapshot, index, -1);
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());

        mSnapshots.remove(index);
        notifyOnChildChanged(ChangeEventType.REMOVED, snapshot, index, -1);
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);

        int newIndex = previousChildKey == null ? 0 : getIndexForKey(previousChildKey) + 1;
        mSnapshots.add(newIndex, snapshot);

        notifyOnChildChanged(ChangeEventType.MOVED, snapshot, newIndex, oldIndex);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        notifyOnDataChanged();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {
        notifyOnError(error);
    }

    private int getIndexForKey(@NonNull String key) {
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

    @NonNull
    @Override
    protected List<DataSnapshot> getSnapshots() {
        return mSnapshots;
    }
}
