package com.firebase.ui.common;

import android.support.annotation.NonNull;

/**
 * Common interface for snapshot parsers.
 *
 * @param <S> snapshot type.
 * @param <T> parsed object type.
 */
public interface BaseSnapshotParser<S, T> {

    /**
     * This method parses the Snapshot into the requested type.
     *
     * @param snapshot the Snapshot to extract the model from
     * @return the model extracted from the DataSnapshot
     */
    @NonNull
    T parseSnapshot(@NonNull S snapshot);

}
