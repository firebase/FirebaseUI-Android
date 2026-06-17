package com.firebase.ui.database.paging;

class DatabasePagingKey {
    private final Object mChildValue;
    private final String mNodeKey;

    DatabasePagingKey(Object childValue, String nodeKey) {
        mChildValue = childValue;
        mNodeKey = nodeKey;
    }

    Object getChildValue() {
        return mChildValue;
    }

    String getNodeKey() {
        return mNodeKey;
    }
}
