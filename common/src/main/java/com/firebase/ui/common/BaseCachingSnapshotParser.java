package com.firebase.ui.common;

import android.support.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link BaseSnapshotParser} that caches results,
 * so parsing a snapshot repeatedly is not expensive.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseCachingSnapshotParser<S, T> implements BaseSnapshotParser<S, T> {

    private Map<String, T> mObjectCache = new HashMap<>();
    private BaseSnapshotParser<S, T> mInnerParser;

    public BaseCachingSnapshotParser(BaseSnapshotParser<S, T> innerParser) {
        mInnerParser = innerParser;
    }

    /**
     * Get a unique identifier for a snapshot, should not depend on snapshot content.
     */
    public abstract String getId(S snapshot);

    @Override
    public T parseSnapshot(S snapshot) {
        String id = getId(snapshot);
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
