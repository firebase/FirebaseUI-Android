/*
 * Firebase UI Bindings Android Library
 *
 * Copyright © 2015 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.firebase.ui;

import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.client.*;

import java.util.ArrayList;

/**
 * This class implements an array-like collection on top of a Firebase location.
 */
class FirebaseArray implements
        ChildEventListener,
        ValueEventListener
{
    public interface OnChangedListener {
        enum EventType { Added, Changed, Removed, Moved, Reset }
        void onChanged(EventType type, int index, int oldIndex);
    }

    public interface OnErrorListener {
        void onError(FirebaseError firebaseError);
    }

    public interface OnSyncStatusChangedListener {
        enum EventType { UnSynced, Synced }
        void onChanged(EventType type);
    }

    private Query mOriginalQuery;
    private Query mQuery;
    private OnChangedListener mOnChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnSyncStatusChangedListener mOnSyncStatusListener;
    private ArrayList<DataSnapshot> mSnapshots;
    private int mPageSize;
    private int mCurrentSize;
    private boolean mSyncing;
    private boolean mOrderASC;

    public FirebaseArray(@NonNull Query query) {
        this(query, 0, true);
    }

    public FirebaseArray(@NonNull Query query, int pageSize, boolean orderASC) {
        mOriginalQuery = query;
        mSnapshots = new ArrayList<DataSnapshot>();
        mPageSize = mCurrentSize = Math.abs(pageSize);
        mOrderASC = orderASC;

        setup();
    }

    public void reset() {
        mCurrentSize = mPageSize;
        mSnapshots.clear();
        setup();
        notifyChangedListeners(OnChangedListener.EventType.Reset, 0);
    }

    public void more() {
        if(mPageSize > 0 && !isSyncing()) {
            mCurrentSize += mPageSize;
            setup();
        }
    }

    public void cleanup() {
        mQuery.removeEventListener((ChildEventListener) this);
        mQuery.removeEventListener((ValueEventListener) this);
    }

    public int getCount() {
        return mSnapshots.size();

    }
    public DataSnapshot getItem(int index) {
        return mSnapshots.get(index);
    }

    private void setup() {
        if(mQuery != null) {
            cleanup();
        }
        if(mPageSize == 0) {
            mQuery = mOriginalQuery;
        }
        else if(mOrderASC == true){
            mQuery = mOriginalQuery.limitToFirst(mCurrentSize);
        }
        else {
            mQuery = mOriginalQuery.limitToLast(mCurrentSize);
        }
        setSyncing(true);
        mQuery.addChildEventListener(this);
        mQuery.addListenerForSingleValueEvent(this);
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

    private boolean isSyncing() {
        return mSyncing;
    }

    private void setSyncing(boolean syncing) {
        if(syncing == mSyncing) {
            return;
        }
        this.mSyncing = syncing;
        if(syncing) {
            notifyOnSyncChangedListeners(OnSyncStatusChangedListener.EventType.UnSynced);
        }
        else {
            notifyOnSyncChangedListeners(OnSyncStatusChangedListener.EventType.Synced);
        }
    }

    // Start of ChildEventListener and ValueEventListener methods

    public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
        int index = (mOrderASC) ? 0 : getCount();
        if (previousChildKey != null) {
            if(mOrderASC) {
                index = getIndexForKey(previousChildKey) + 1;
            }
            else {
                index = getIndexForKey(previousChildKey);
            }
        }
        if(mOrderASC &&
                index < getCount()  &&
                mSnapshots.get(index).getKey().equals(snapshot.getKey())) {
            return;
        }
        else if(!mOrderASC &&
                index < getCount() + 1  &&
                index > 0 &&
                mSnapshots.get(index - 1).getKey().equals(snapshot.getKey())) {
            return;
        }

        mSnapshots.add(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Added, index);
    }

    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.set(index, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Changed, index);
    }

    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(index);
        notifyChangedListeners(OnChangedListener.EventType.Removed, index);
    }

    public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);
        notifyChangedListeners(OnChangedListener.EventType.Moved, newIndex, oldIndex);
    }

    public void onDataChange(DataSnapshot dataSnapshot) {
        setSyncing(false);
    }

    public void onCancelled(FirebaseError firebaseError) {
        notifyOnErrorListeners(firebaseError);
    }

    // End of ChildEventListener and ValueEventListener methods

    public void setOnChangedListener(OnChangedListener listener) {
        mOnChangedListener = listener;
    }
    protected void notifyChangedListeners(OnChangedListener.EventType type, int index) {
        notifyChangedListeners(type, index, -1);
    }
    protected void notifyChangedListeners(OnChangedListener.EventType type, int index, int oldIndex) {
        if (mOnChangedListener != null) {
            mOnChangedListener.onChanged(type, index, oldIndex);
        }
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }
    public void notifyOnErrorListeners(FirebaseError firebaseError) {
        if(mOnErrorListener != null) {
            mOnErrorListener.onError(firebaseError);
        }
    }

    public void setOnSyncStatusChangedListener(OnSyncStatusChangedListener listener) {
        mOnSyncStatusListener = listener;
        notifyOnSyncChangedListeners(isSyncing() ? OnSyncStatusChangedListener.EventType.UnSynced : OnSyncStatusChangedListener.EventType.Synced);
    }
    protected void notifyOnSyncChangedListeners(OnSyncStatusChangedListener.EventType type) {
        if(mOnSyncStatusListener != null) {
            mOnSyncStatusListener.onChanged(type);
        }
    }
}
