package com.firebase.ui.firestore;

import com.firebase.ui.common.BaseCachingSnapshotParser;
import com.firebase.ui.common.BaseSnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Implementation of {@link BaseCachingSnapshotParser} for {@link DocumentSnapshot}.
 */
public class CachingSnapshotParser<T> extends BaseCachingSnapshotParser<DocumentSnapshot, T>
        implements SnapshotParser<T> {

    public CachingSnapshotParser(BaseSnapshotParser<DocumentSnapshot, T> parser) {
        super(parser);
    }

    @Override
    public String getId(DocumentSnapshot snapshot) {
        return snapshot.getId();
    }
}
