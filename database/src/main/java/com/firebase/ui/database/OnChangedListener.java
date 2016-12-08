package com.firebase.ui.database;

import com.google.firebase.database.DatabaseError;

public interface OnChangedListener {
    enum EventType {ADDED, CHANGED, REMOVED, MOVED}

    void onChanged(EventType type, int index, int oldIndex);

    void onCancelled(DatabaseError databaseError);
}
