package com.firebase.ui.common;


public interface BaseSnapshotParser<S, T> {

    /**
     * This method parses the Snapshot into the requested type.
     *
     * @param snapshot the Snapshot to extract the model from
     * @return the model extracted from the DataSnapshot
     */
    T parseSnapshot(S snapshot);

}
