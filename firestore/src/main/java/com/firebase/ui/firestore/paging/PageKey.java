/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageKey key = (PageKey) o;
        if (mStartAfter == null && key.mStartAfter == null &&
                mEndBefore == null && key.mEndBefore == null)
            return true;
        return mStartAfter.getId().equals(key.mStartAfter.getId()) &&
                mEndBefore.getId().equals(key.mEndBefore.getId());
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
