package com.firebase.uidemo.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Common interface for chat messages, helps share code between RTDB and Firestore examples.
 */
public abstract class AbstractChat {

    @Nullable
    public abstract String getName();

    public abstract void setName(@Nullable String name);

    @Nullable
    public abstract String getMessage();

    public abstract void setMessage(@Nullable String message);

    @NonNull
    public abstract String getUid();

    public abstract void setUid(@NonNull String uid);

    @Override
    public abstract boolean equals(@Nullable Object obj);

    @Override
    public abstract int hashCode();

}
