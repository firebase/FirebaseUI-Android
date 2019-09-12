package com.firebase.ui.firestore.paging;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Key for Firestore pagination. Holds the DocumentSnapshot(s) that bound the page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PageKey {

    private final DocumentSnapshot mStartAfter;
    private final DocumentSnapshot mEndBefore;

    public PageKey(@Nullable DocumentSnapshot startAfter, @Nullable DocumentSnapshot endBefore) {
        mStartAfter = startAfter;
        mEndBefore = endBefore;
    }

    @NonNull
    public Query getPageQuery(@NonNull Query baseQuery, int size) {
        Query pageQuery = baseQuery;

        if (mStartAfter != null) {
            pageQuery = pageQuery.startAfter(mStartAfter);
        }

        if (mEndBefore != null) {
            pageQuery = pageQuery.endBefore(mEndBefore);
        } else {
            pageQuery = pageQuery.limit(size);
        }

        return pageQuery;
    }

    @Override
    @NonNull
    public String toString() {
        String startAfter = mStartAfter == null ? null : mStartAfter.getId();
        String endBefore = mEndBefore == null ? null : mEndBefore.getId();
        return "PageKey{" +
                "StartAfter=" + startAfter +
                ", EndBefore=" + endBefore +
                '}';
    }
}
