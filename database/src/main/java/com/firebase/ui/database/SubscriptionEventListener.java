package com.firebase.ui.database;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface SubscriptionEventListener {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EventType.ADDED, EventType.REMOVED})
    @interface EventType {
        int ADDED = 0, REMOVED = 1;
    }

    void onSubscriptionAdded();

    void onSubscriptionRemoved();
}
