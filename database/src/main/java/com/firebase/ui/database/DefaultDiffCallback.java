package com.firebase.ui.database;

import android.annotation.SuppressLint;

import com.google.firebase.database.DataSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Default diff callback implementation for Firebase Data snapshots.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultDiffCallback<T> extends DiffUtil.ItemCallback<T> {

    @Override
    public boolean areItemsTheSame(@NonNull T oldItem,
                                   @NonNull T newItem) {
        return oldItem.equals(newItem);
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull T oldItem,
                                      @NonNull T newItem) {

        return oldItem.equals(newItem);
    }
}
