/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.database;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.firebase.ui.common.ChangeEventType;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * This class is a generic way of backing an Android {@link android.widget.ListView} with a Firebase
 * location. It handles all of the child events at the given Firebase location. It marshals received
 * data into the given class type.
 * <p>
 * See the <a href="https://github.com/firebase/FirebaseUI-Android/blob/master/database/README.md">README</a>
 * for an in-depth tutorial on how to set up the FirebaseListAdapter.
 *
 * @param <T> The class type to use as a model for the data contained in the children of the given
 *            Firebase location
 */
public abstract class FirebaseListAdapter<T> extends BaseAdapter implements FirebaseAdapter<T> {
    private static final String TAG = "FirebaseListAdapter";

    private final ObservableSnapshotArray<T> mSnapshots;
    protected final int mLayout;

    public FirebaseListAdapter(@NonNull FirebaseListOptions<T> options) {
        mSnapshots = options.getSnapshots();
        mLayout = options.getLayout();

        if (options.getOwner() != null) {
            options.getOwner().getLifecycle().addObserver(this);
        }
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        mSnapshots.removeChangeEventListener(this);
        notifyDataSetChanged();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void cleanup(LifecycleOwner source) {
        source.getLifecycle().removeObserver(this);
    }

    @Override
    public void onChildChanged(@NonNull ChangeEventType type,
                               @NonNull DataSnapshot snapshot,
                               int newIndex,
                               int oldIndex) {
        notifyDataSetChanged();
    }

    @Override
    public void onDataChanged() {
    }

    @Override
    public void onError(@NonNull DatabaseError error) {
        Log.w(TAG, error.toException());
    }

    @NonNull
    @Override
    public ObservableSnapshotArray<T> getSnapshots() {
        return mSnapshots;
    }

    @NonNull
    @Override
    public T getItem(int position) {
        return mSnapshots.get(position);
    }

    @NonNull
    @Override
    public DatabaseReference getRef(int position) {
        return mSnapshots.getSnapshot(position).getRef();
    }

    @Override
    public int getCount() {
        return mSnapshots.size();
    }

    @Override
    public long getItemId(int i) {
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        return mSnapshots.getSnapshot(i).getKey().hashCode();
    }

    @Override
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(mLayout, parent, false);
        }

        T model = getItem(position);

        // Call out to subclass to marshall this model into the provided view
        populateView(convertView, model, position);
        return convertView;
    }

    /**
     * Each time the data at the given Firebase location changes, this method will be called for
     * each item that needs to be displayed. The first two arguments correspond to the mLayout and
     * mModelClass given to the constructor of this class. The third argument is the item's position
     * in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     *
     * @param v        The view to populate
     * @param model    The object containing the data used to populate the view
     * @param position The position in the list of the view being populated
     */
    protected abstract void populateView(@NonNull View v, @NonNull T model, int position);
}
