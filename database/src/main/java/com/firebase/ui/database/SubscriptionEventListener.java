package com.firebase.ui.database;

import android.support.annotation.IntDef;

public interface SubscriptionEventListener {
    @IntDef({EventType.ADDED, EventType.REMOVED})
    @interface EventType {
        int ADDED = 0, REMOVED = 1;
    }

    void onSubscriptionAdded();

    void onSubscriptionRemoved();
}
