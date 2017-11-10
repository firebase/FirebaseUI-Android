package com.firebase.ui.common;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.LruCache;

/**
 * Implementation of {@link BaseSnapshotParser} that caches results, so parsing a snapshot
 * repeatedly is not expensive.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseCachingSnapshotParser<S, T> implements BaseSnapshotParser<S, T> {

    private static final int MAX_CACHE_SIZE = 100;

    private final LruCache<String, T> mObjectCache = new LruCache<>(MAX_CACHE_SIZE);
    private final BaseSnapshotParser<S, T> mParser;

    public BaseCachingSnapshotParser(@NonNull BaseSnapshotParser<S, T> parser) {
        mParser = parser;
    }

    /**
     * Get a unique identifier for a snapshot, should not depend on snapshot content.
     */
    @NonNull
    public abstract String getId(@NonNull S snapshot);

    @NonNull
    @Override
    public T parseSnapshot(@NonNull S snapshot) {
        String id = getId(snapshot);
        T result = mObjectCache.get(id);
        if (result == null) {
            T object = mParser.parseSnapshot(snapshot);
            mObjectCache.put(id, object);
            result = object;
        }
        return result;
    }

    /**
     * Clear all data in the cache.
     */
    public void clear() {
        mObjectCache.evictAll();
    }

    /**
     * Invalidate the cache for a certain document.
     */
    public void invalidate(@NonNull S snapshot) {
        mObjectCache.remove(getId(snapshot));
    }

}
