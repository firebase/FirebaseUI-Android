package com.firebase.ui.database;

import com.firebase.ui.common.BaseCachingSnapshotParser;
import com.firebase.ui.common.BaseSnapshotParser;
import com.google.firebase.database.DataSnapshot;

import androidx.annotation.NonNull;

/**
 * Implementation of {@link BaseCachingSnapshotParser} for {@link DataSnapshot}.
 */
public class CachingSnapshotParser<T> extends BaseCachingSnapshotParser<DataSnapshot, T> {

    public CachingSnapshotParser(@NonNull BaseSnapshotParser<DataSnapshot, T> parser) {
        super(parser);
    }

    @NonNull
    @Override
    public String getId(@NonNull DataSnapshot snapshot) {
        return snapshot.getKey();
    }
}
