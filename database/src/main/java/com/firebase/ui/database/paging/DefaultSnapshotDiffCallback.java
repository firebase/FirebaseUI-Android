package com.firebase.ui.database.paging;

import android.annotation.SuppressLint;

import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Default diff callback implementation for Firebase Data snapshots.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultSnapshotDiffCallback<T> extends DiffUtil.ItemCallback<DataSnapshot> {

    private final SnapshotParser<T> mParser;

    public DefaultSnapshotDiffCallback(@NonNull SnapshotParser<T> parser) {
        mParser = parser;
    }

    @Override
    public boolean areItemsTheSame(@NonNull DataSnapshot oldItem,
                                   @NonNull DataSnapshot newItem) {
        return oldItem.getKey().equals(newItem.getKey());
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull DataSnapshot oldItem,
                                      @NonNull DataSnapshot newItem) {
        T oldModel = mParser.parseSnapshot(oldItem);
        T newModel = mParser.parseSnapshot(newItem);

        return oldModel.equals(newModel);
    }
}
