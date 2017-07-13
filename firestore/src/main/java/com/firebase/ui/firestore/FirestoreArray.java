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
 * TODO(samstern): What to do about sorting?
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
            switch (change.getType()) {
                case ADDED:
                    onDocumentAdded(doc);
                    break;
                case REMOVED:
                    onDocumentRemoved(doc);
                    break;
                case MODIFIED:
                    onDocumentModified(doc);
                    break;
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

    private void onDocumentAdded(DocumentSnapshot doc) {
        mSnapshots.add(doc);
        notifyOnChildChanged(DocumentChange.Type.ADDED, doc, mSnapshots.size() - 1);
    }

    private void onDocumentRemoved(DocumentSnapshot doc) {
        int ind = getDocumentIndex(doc);
        if (ind >= 0) {
            mSnapshots.remove(ind);
            notifyOnChildChanged(DocumentChange.Type.REMOVED, doc, ind);
        }
    }

    private void onDocumentModified(DocumentSnapshot doc) {
        int ind = getDocumentIndex(doc);
        if (ind >= 0) {
            mSnapshots.set(ind, doc);
            notifyOnChildChanged(DocumentChange.Type.MODIFIED, doc, ind);
        }
    }

    private void notifyOnChildChanged(DocumentChange.Type type,
                                      DocumentSnapshot snapshot,
                                      int index) {

        for (ChangeEventListener listener : mListeners) {
            listener.onChildChanged(type, snapshot, index);
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

    private int getDocumentIndex(DocumentSnapshot doc) {
        String id = doc.getReference().getId();
        for (int i = 0; i < mSnapshots.size(); i++) {
            if (mSnapshots.get(i).getReference().getId().equals(id)) {
                return i;
            }
        }

        return -1;
    }
}
