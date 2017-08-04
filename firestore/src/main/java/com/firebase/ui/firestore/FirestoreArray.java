package com.firebase.ui.firestore;

import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.database.BaseObservableSnapshotArray;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO(samstern): Document
 */
public class FirestoreArray<T>
        extends BaseObservableSnapshotArray<DocumentSnapshot, ChangeEventListener, T>
        implements EventListener<QuerySnapshot>  {

    private static final String TAG = "FirestoreArray";

    private Query mQuery;
    private ListenerRegistration mRegistration;

    private List<DocumentSnapshot> mSnapshots;

    public FirestoreArray(Query query, final Class<T> modelClass) {
        this(query, new SnapshotParser<T>() {
            @Override
            public T parseSnapshot(DocumentSnapshot snapshot) {
                return snapshot.toObject(modelClass);
            }
        });
    }

    // TODO: What about caching?
    // TODO: What about intermediate OSA class?
    public FirestoreArray(Query query, SnapshotParser<T> parser) {
        super(parser);

        mQuery = query;
        mSnapshots = new ArrayList<>();
    }

    public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
        boolean wasListening = isListening();
        super.addChangeEventListener(listener);

        // Only start listening once we've added our first listener
        if (!wasListening) {
            startListening();
        }

        // TODO: Look at what observable snapshot array does here.

        return listener;
    }

    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        super.removeChangeEventListener(listener);

        // Stop listening when we have no listeners
        if (!isListening()) {
            stopListening();
        }
    }

    @Override
    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
        if (e != null) {
            Log.w(TAG, "Error in snapshot listener", e);
            notifyOnError(e);
            return;
        }

        // Break down each document event
        List<DocumentChange> changes = snapshots.getDocumentChanges();
        for (DocumentChange change : changes) {
            DocumentSnapshot doc = change.getDocument();
            
            DocumentChange.Type changeType = change.getType();
            int oldIndex = change.getOldIndex();
            int newIndex = change.getNewIndex();
            
            if (changeType == DocumentChange.Type.MODIFIED) {
                if (oldIndex == newIndex) {
                    Log.d(TAG, "Modified (inplace): " + oldIndex);

                    mSnapshots.set(oldIndex, doc);
                    notifyOnChildChanged(ChangeEventListener.Type.MODIFIED, doc,
                            oldIndex, newIndex);
                } else {
                    Log.d(TAG, "Modified (moved): " + oldIndex + " --> " + newIndex);

                    mSnapshots.remove(oldIndex);
                    mSnapshots.add(newIndex, doc);
                    notifyOnChildChanged(ChangeEventListener.Type.MOVED, doc,
                            oldIndex, newIndex);
                }
            } else if (changeType == DocumentChange.Type.REMOVED) {
                Log.d(TAG, "Removed: " + oldIndex);

                mSnapshots.remove(oldIndex);
                notifyOnChildChanged(ChangeEventListener.Type.REMOVED, doc,
                        oldIndex, -1);
            } else if (changeType == DocumentChange.Type.ADDED) {
                Log.d(TAG, "Added: " + newIndex);

                mSnapshots.add(newIndex, doc);
                notifyOnChildChanged(ChangeEventListener.Type.ADDED, doc,
                        -1, newIndex);
            }
        }

        notifyOnDataChanged();
    }


    @Override
    protected List<DocumentSnapshot> getSnapshots() {
        return mSnapshots;
    }

    private void startListening() {
        mRegistration = mQuery.addSnapshotListener(this);
    }

    private void stopListening() {
        if (mRegistration != null) {
            mRegistration.remove();
            mRegistration = null;
        }

        mSnapshots.clear();
    }

    private void notifyOnChildChanged(ChangeEventListener.Type type,
                                      DocumentSnapshot snapshot,
                                      int oldIndex,
                                      int newIndex) {

        for (ChangeEventListener listener : mListeners) {
            listener.onChildChanged(type, snapshot, oldIndex, newIndex);
        }
    }

    private void notifyOnError(FirebaseFirestoreException e) {
        for (ChangeEventListener listener : mListeners) {
            listener.onError(e);
        }
    }

    private void notifyOnDataChanged() {
        for (ChangeEventListener listener : mListeners) {
            listener.onDataChanged();
        }
    }
}
