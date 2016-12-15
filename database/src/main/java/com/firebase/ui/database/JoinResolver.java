package com.firebase.ui.database;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;

interface JoinResolver {
    /**
     * Called after an {@code onChildAdded} event from {@code keyRef}.
     *
     * @param keySnapshot      The snapshot supplied in {@code onChildAdded}.
     * @param previousChildKey The previous child's key supplied in {@code onChildAdded}.
     * @return A query containing the joined data from the {@code keyRef}'s indexed snapshot.
     * <p>Without any customization, a query on the child from your {@code dataRef} with the key
     * found in {@code keySnapshot} will be returned.
     */
    Query onJoin(DataSnapshot keySnapshot, String previousChildKey);

    /**
     * Called after an {@code onChildRemoved} event from {@code keyRef}.
     *
     * @param keySnapshot The snapshot supplied in {@code onChildRemoved}.
     * @return The same query supplied in {@code onJoin} for the given {@code keySnapshot}.
     */
    Query onDisjoin(DataSnapshot keySnapshot);

    /**
     * Called when a key in {@code keyRef} could not be found in {@code dataRef}.
     *
     * @param index    The index of a {@code snapshot} in {@code keyRef} that could not be found in {@code dataRef}.
     * @param snapshot The snapshot who's key could not be found in {@code dataRef}.
     */
    void onJoinFailed(int index, DataSnapshot snapshot);
}
