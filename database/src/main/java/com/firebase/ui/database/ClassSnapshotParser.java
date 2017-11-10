package com.firebase.ui.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.common.Preconditions;
import com.google.firebase.database.DataSnapshot;

/**
 * A convenience implementation of {@link SnapshotParser} that converts a {@link DataSnapshot} to
 * the parametrized class via {@link DataSnapshot#getValue(Class)}.
 *
 * @param <T> the POJO class to create from snapshots.
 */
public class ClassSnapshotParser<T> implements SnapshotParser<T> {
    private Class<T> mClass;

    public ClassSnapshotParser(@NonNull Class<T> clazz) {
        mClass = Preconditions.checkNotNull(clazz);
    }

    @Nullable
    @Override
    public T parseSnapshot(@NonNull DataSnapshot snapshot) {
        // In FirebaseUI controlled usages, we can guarantee that our getValue calls will be nonnull
        // because we check for nullity with ValueEventListeners and use ChildEventListeners.
        // However, since this API is public, devs could use it for any snapshot including null
        // ones. Hence the nullability discrepancy.
        return snapshot.getValue(mClass);
    }
}
