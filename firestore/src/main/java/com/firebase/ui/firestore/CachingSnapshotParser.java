package com.firebase.ui.firestore;

import android.support.annotation.RestrictTo;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link SnapshotParser} that caches results,
 * so parsing a snapshot repeatedly is not expensive.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CachingSnapshotParser<T> implements SnapshotParser<T> {

    private Map<String, T> mObjectCache = new HashMap<>();
    private SnapshotParser<T> mInnerParser;

    public CachingSnapshotParser(SnapshotParser<T> innerParser) {
        mInnerParser = innerParser;
    }

    @Override
    public T parseSnapshot(DocumentSnapshot snapshot) {
        String id = snapshot.getId();
        if (mObjectCache.containsKey(id)) {
            return mObjectCache.get(id);
        } else {
            T object = mInnerParser.parseSnapshot(snapshot);
            mObjectCache.put(id, object);
            return object;
        }
    }

    /**
     * Clear all data in the cache.
     */
    public void clearData() {
        mObjectCache.clear();
    }

    /**
     * Invalidate the cache for a certain document ID.
     */
    public void invalidate(String id) {
        mObjectCache.remove(id);
    }

}
