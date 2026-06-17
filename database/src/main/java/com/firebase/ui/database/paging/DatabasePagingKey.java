package com.firebase.ui.database.paging;

import java.util.Objects;

public class DatabasePagingKey {
    private final Object mChildValue;
    private final String mNodeKey;

    public DatabasePagingKey(Object childValue, String nodeKey) {
        mChildValue = childValue;
        mNodeKey = nodeKey;
    }

    public Object getChildValue() {
        return mChildValue;
    }

    public String getNodeKey() {
        return mNodeKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabasePagingKey that = (DatabasePagingKey) o;
        return Objects.equals(mChildValue, that.mChildValue) &&
                Objects.equals(mNodeKey, that.mNodeKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mChildValue, mNodeKey);
    }
}
