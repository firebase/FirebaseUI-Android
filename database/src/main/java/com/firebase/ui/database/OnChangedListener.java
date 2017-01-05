package com.firebase.ui.database;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public interface OnChangedListener {
    /**
     * ADDED: an onChildAdded event was received
     * <p>
     * CHANGED: an onChildChanged event was received
     * <p>
     * REMOVED: an onChildRemoved event was received
     * <p>
     * MOVED: an onChildMoved event was received
     *
     * @see ChildEventListener#onChildAdded(DataSnapshot, String)
     * @see ChildEventListener#onChildChanged(DataSnapshot, String)
     * @see ChildEventListener#onChildRemoved(DataSnapshot)
     * @see ChildEventListener#onChildMoved(DataSnapshot, String)
     */
    enum EventType {ADDED, CHANGED, REMOVED, MOVED}

    /**
     * A callback for when a child has changed in FirebaseArray.
     *
     * @param type The type of event received
     * @param index The index at which the change occurred
     * @param oldIndex If {@code type} is a moved event, the previous index of the moved child.
     *                 For any other event, {@code oldIndex} will be -1.
     */
    void onChanged(EventType type, int index, int oldIndex);

    /**
     * This method will be triggered in the event that this listener either failed at the server,
     * or is removed as a result of the security and Firebase Database rules.
     *
     * @param error A description of the error that occurred
     */
    void onCancelled(DatabaseError error);
}
