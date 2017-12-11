package com.firebase.ui.firestore;

import android.support.annotation.NonNull;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryListenOptions;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a Firestore query as an observable list of objects.
 */
public class FirestoreArray<T> extends ObservableSnapshotArray<T>
        implements EventListener<QuerySnapshot> {
    private final Query mQuery;
    private final QueryListenOptions mOptions;
    private ListenerRegistration mRegistration;

    private final List<DocumentSnapshot> mSnapshots = new ArrayList<>();

    /**
     * Create a new FirestoreArray.
     *
     * @param query  query to listen to.
     * @param parser parser for DocumentSnapshots.
     * @see ObservableSnapshotArray#ObservableSnapshotArray(SnapshotParser)
     */
    public FirestoreArray(@NonNull Query query, @NonNull SnapshotParser<T> parser) {
        this(query, new QueryListenOptions(), parser);
    }

    /**
     * @param options options for the query listen.
     * @see #FirestoreArray(Query, SnapshotParser)
     */
    public FirestoreArray(@NonNull Query query,
                          @NonNull QueryListenOptions options,
                          @NonNull SnapshotParser<T> parser) {
        super(parser);
        mQuery = query;
        mOptions = options;
    }

    @NonNull
    @Override
    protected List<DocumentSnapshot> getSnapshots() {
        return mSnapshots;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mRegistration = mQuery.addSnapshotListener(mOptions, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRegistration.remove();
        mRegistration = null;
    }

    @Override
    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
        if (e != null) {
            notifyOnError(e);
            return;
        }

        // Break down each document event
        List<DocumentChange> changes = snapshots.getDocumentChanges();
        for (DocumentChange change : changes) {
            switch (change.getType()) {
                case ADDED:
                    onDocumentAdded(change);
                    break;
                case REMOVED:
                    onDocumentRemoved(change);
                    break;
                case MODIFIED:
                    onDocumentModified(change);
                    break;
            }
        }

        notifyOnDataChanged();
    }

    private void onDocumentAdded(DocumentChange change) {
        mSnapshots.add(change.getNewIndex(), change.getDocument());
        notifyOnChildChanged(ChangeEventType.ADDED, change.getDocument(), change.getNewIndex(), -1);
    }

    private void onDocumentRemoved(DocumentChange change) {
        mSnapshots.remove(change.getOldIndex());
        notifyOnChildChanged(
                ChangeEventType.REMOVED, change.getDocument(), -1, change.getOldIndex());
    }

    private void onDocumentModified(DocumentChange change) {
        if (change.getOldIndex() == change.getNewIndex()) {
            // Document modified only
            mSnapshots.set(change.getNewIndex(), change.getDocument());
            notifyOnChildChanged(ChangeEventType.CHANGED, change.getDocument(),
                    change.getNewIndex(), change.getNewIndex());
        } else {
            // Document moved and possibly also modified
            mSnapshots.remove(change.getOldIndex());
            mSnapshots.add(change.getNewIndex(), change.getDocument());

            notifyOnChildChanged(ChangeEventType.MOVED, change.getDocument(),
                    change.getNewIndex(), change.getOldIndex());
            notifyOnChildChanged(ChangeEventType.CHANGED, change.getDocument(),
                    change.getNewIndex(), change.getNewIndex());
        }
    }
}
