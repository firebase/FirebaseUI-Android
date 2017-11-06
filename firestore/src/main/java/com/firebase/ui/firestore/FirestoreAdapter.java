package com.firebase.ui.firestore;

import android.support.annotation.RestrictTo;

import com.firebase.ui.common.Adapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FirestoreAdapter<T> extends Adapter<ObservableSnapshotArray<T>,
        DocumentSnapshot, FirebaseFirestoreException,
        T>, ChangeEventListener {}
