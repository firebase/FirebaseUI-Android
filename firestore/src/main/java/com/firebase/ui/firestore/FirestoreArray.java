package com.firebase.ui.firestore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a Firestore query as an observable list of objects.
 */
public class FirestoreArray<T> extends ObservableSnapshotArray<T>
        implements EventListener<QuerySnapshot> {
    private final Query mQuery;
    private final MetadataChanges mMetadataChanges;
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
        this(query, MetadataChanges.EXCLUDE, parser);
    }

    /**
     * @param changes metadata options for the query listen.
     * @see #FirestoreArray(Query, SnapshotParser)
     */
    public FirestoreArray(@NonNull Query query,
                          @NonNull MetadataChanges changes,
                          @NonNull SnapshotParser<T> parser) {
        super(parser);
        mQuery = query;
        mMetadataChanges = changes;
    }

    @NonNull
    @Override
    protected List<DocumentSnapshot> getSnapshots() {
        return mSnapshots;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mRegistration = mQuery.addSnapshotListener(mMetadataChanges, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRegistration.remove();
        mRegistration = null;
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
        if (e != null) {
            notifyOnError(e);
            return;
        }

        // Break down each document event
        List<DocumentChange> changes = snapshots.getDocumentChanges(mMetadataChanges);
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
        QueryDocumentSnapshot snapshot = change.getDocument();
        mSnapshots.add(change.getNewIndex(), snapshot);
        notifyOnChildChanged(ChangeEventType.ADDED, snapshot, change.getNewIndex(), -1);
    }

    private void onDocumentRemoved(DocumentChange change) {
        mSnapshots.remove(change.getOldIndex());
        QueryDocumentSnapshot snapshot = change.getDocument();
        notifyOnChildChanged(ChangeEventType.REMOVED, snapshot, -1, change.getOldIndex());
    }

    private void onDocumentModified(DocumentChange change) {
        QueryDocumentSnapshot snapshot = change.getDocument();
        if (change.getOldIndex() == change.getNewIndex()) {
            // Document modified only
            mSnapshots.set(change.getNewIndex(), snapshot);
            notifyOnChildChanged(ChangeEventType.CHANGED, snapshot,
                    change.getNewIndex(), change.getNewIndex());
        } else {
            // Document moved and possibly also modified
            mSnapshots.remove(change.getOldIndex());
            mSnapshots.add(change.getNewIndex(), snapshot);

            notifyOnChildChanged(ChangeEventType.MOVED, snapshot,
                    change.getNewIndex(), change.getOldIndex());
            notifyOnChildChanged(ChangeEventType.CHANGED, snapshot,
                    change.getNewIndex(), change.getNewIndex());
        }
    }
}
