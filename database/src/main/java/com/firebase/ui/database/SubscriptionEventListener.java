package com.firebase.ui.database;

public interface SubscriptionEventListener {
    enum EventType {
        ADDED,
        REMOVED
    }

    void onSubscriptionAdded();

    void onSubscriptionRemoved();
}
