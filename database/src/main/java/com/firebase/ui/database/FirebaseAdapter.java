package com.firebase.ui.database;

import android.arch.lifecycle.LifecycleObserver;
import android.support.annotation.RestrictTo;

import com.google.firebase.database.DatabaseReference;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FirebaseAdapter<T> extends ChangeEventListener, LifecycleObserver {
    /**
     * If you need to do some setup before the adapter starts listening for change events in the
     * database, do so it here and then call {@code super.startListening()}.
     */
    void startListening();

    /**
     * Removes listeners and clears all items in the backing {@link FirebaseArray}.
     */
    void cleanup();

    T getItem(int position);

    DatabaseReference getRef(int position);
}
