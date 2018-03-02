package com.firebase.ui.firestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * See {@link FirestorePagingAdapter}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Page implements OnCompleteListener<QuerySnapshot> {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface Listener {

        void onPageStateChanged(Page page, Page.State state);
        void onPageError(Page page, Exception ex);

    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum State {
        LOADING,
        LOADED,
        UNLOADED
    }

    private final int mIndex;
    private final Listener mListener;

    private State mState;
    private DocumentSnapshot mFirstSnapshot;

    private List<DocumentSnapshot> mSnapshots = new ArrayList<>();

    public Page(int index, Listener listener) {
        mIndex = index;
        mListener = listener;

        mState = State.UNLOADED;
    }

    public void load(Query query) {
        if (mState == State.LOADING) {
            return;
        }

        setState(State.LOADING);
        query.get().addOnCompleteListener(this);
    }

    public void unload() {
        setState(State.UNLOADED);

        // Note: very important that this is after setState so that the size reported
        // in the unloaded listener is accurate.
        mSnapshots.clear();
    }

    @Override
    public void onComplete(@NonNull Task<QuerySnapshot> task) {
        if (!task.isSuccessful()) {
            if (mListener != null) {
                mListener.onPageError(this, task.getException());
            }
        }

        // Add all snapshots
        mSnapshots.addAll(task.getResult().getDocuments());

        // Set first in page
        if (mSnapshots.isEmpty()) {
            mFirstSnapshot = null;
        } else {
            mFirstSnapshot = mSnapshots.get(0);
        }

        // Mark page as loaded
        setState(State.LOADED);
    }

    public int getIndex() {
        return mIndex;
    }

    public State getState() {
        return mState;
    }

    public DocumentSnapshot get(int index) {
        return mSnapshots.get(index);
    }

    public int size() {
        return mSnapshots.size();
    }

    @Nullable
    public DocumentSnapshot getFirst() {
        return mFirstSnapshot;
    }

    @Nullable
    public DocumentSnapshot getLast() {
        if (mSnapshots == null || mSnapshots.isEmpty()) {
            return null;
        }
        return mSnapshots.get(mSnapshots.size() - 1);
    }

    private void setState(State state) {
        mState = state;
        if (mListener != null) {
            mListener.onPageStateChanged(this, state);
        }
    }
}
