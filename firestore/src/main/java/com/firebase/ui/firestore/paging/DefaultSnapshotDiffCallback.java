package com.firebase.ui.firestore.paging;

import android.support.annotation.RestrictTo;
import android.support.v7.util.DiffUtil;

import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Default diff callback implementation for Firestore snapshots.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultSnapshotDiffCallback<T> extends DiffUtil.ItemCallback<DocumentSnapshot> {

    private final SnapshotParser<T> mParser;

    public DefaultSnapshotDiffCallback(SnapshotParser<T> parser) {
        mParser = parser;
    }

    @Override
    public boolean areItemsTheSame(DocumentSnapshot oldItem, DocumentSnapshot newItem) {
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(DocumentSnapshot oldItem, DocumentSnapshot newItem) {
        T oldModel = mParser.parseSnapshot(oldItem);
        T newModel = mParser.parseSnapshot(newItem);

        return oldModel.equals(newModel);
    }
}
