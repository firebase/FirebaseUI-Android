package com.firebase.ui.database;

import com.firebase.ui.common.BaseCachingSnapshotParser;
import com.firebase.ui.common.BaseSnapshotParser;
import com.google.firebase.database.DataSnapshot;

/**
 * Implementation of {@link BaseCachingSnapshotParser} for {@link DataSnapshot}.
 */
public class CachingSnapshotParser<T> extends BaseCachingSnapshotParser<DataSnapshot, T> {

    public CachingSnapshotParser(BaseSnapshotParser<DataSnapshot, T> innerParser) {
        super(innerParser);
    }

    @Override
    public String getId(DataSnapshot snapshot) {
        return snapshot.getKey();
    }
}
