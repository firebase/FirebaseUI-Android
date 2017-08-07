package com.firebase.ui.database;

import com.firebase.ui.common.BaseChangeEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

/**
 * Listener for changes to {@link FirebaseArray}.
 */
public interface ChangeEventListener extends BaseChangeEventListener<DataSnapshot, DatabaseError> {}
