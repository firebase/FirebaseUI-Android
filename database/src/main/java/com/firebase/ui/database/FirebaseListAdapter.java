/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import android.app.Activity;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/**
 * This class is a generic way of backing an Android ListView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type. Extend this class and provide an implementation of {@code populateView}, which will be given an
 * instance of your list item mLayout and an instance your class that holds your data.
 * Simply populate the view however you like and this class will handle updating the list as the data changes.
 * <p>
 * <pre>
 *     DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
 *     ListAdapter adapter = new FirebaseListAdapter<ChatMessage>(
 *              this, ChatMessage.class, android.R.layout.two_line_list_item, ref)
 *     {
 *         protected void populateView(View view, ChatMessage chatMessage, int position)
 *         {
 *             ((TextView)view.findViewById(android.R.id.text1)).setText(chatMessage.getName());
 *             ((TextView)view.findViewById(android.R.id.text2)).setText(chatMessage.getMessage());
 *         }
 *     };
 *     listView.setListAdapter(adapter);
 * </pre>
 *
 * @param <T> The class type to use as a model for the data
 *            contained in the children of the given Firebase location
 */
public abstract class FirebaseListAdapter<T> extends BaseAdapter {
    private static final String TAG = "FirebaseListAdapter";

    private FirebaseArray mSnapshots;
    private final Class<T> mModelClass;
    protected Activity mActivity;
    protected int mLayout;

    FirebaseListAdapter(Activity activity,
                        Class<T> modelClass,
                        @LayoutRes int modelLayout,
                        FirebaseArray snapshots) {
        mActivity = activity;
        mModelClass = modelClass;
        mLayout = modelLayout;
        mSnapshots = snapshots;

        mSnapshots.setOnChangedListener(new ChangeEventListener() {
            @Override
            public void onChildChanged(EventType type, int index, int oldIndex) {
                FirebaseListAdapter.this.onChildChanged(type, index, oldIndex);
            }

            @Override
            public void onDataChanged() {
                FirebaseListAdapter.this.onDataChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                FirebaseListAdapter.this.onCancelled(error);
            }
        });
    }

    /**
     * @param activity    The activity containing the ListView
     * @param modelClass  Firebase will marshall the data at a location into
     *                    an instance of a class that you provide
     * @param modelLayout This is the layout used to represent a single list item.
     *                    You will be responsible for populating an instance of the corresponding
     *                    view with the data from an instance of modelClass.
     * @param ref         The Firebase location to watch for data changes. Can also be a slice of a location,
     *                    using some combination of {@code limit()}, {@code startAt()}, and {@code endAt()}.
     */
    public FirebaseListAdapter(Activity activity,
                               Class<T> modelClass,
                               int modelLayout,
                               Query ref) {
        this(activity, modelClass, modelLayout, new FirebaseArray(ref));
    }

    public void cleanup() {
        mSnapshots.cleanup();
    }

    @Override
    public int getCount() {
        return mSnapshots.getCount();
    }

    @Override
    public T getItem(int position) {
        return parseSnapshot(mSnapshots.getItem(position));
    }

    /**
     * This method parses the DataSnapshot into the requested type. You can override it in subclasses
     * to do custom parsing.
     *
     * @param snapshot the DataSnapshot to extract the model from
     * @return the model extracted from the DataSnapshot
     */
    protected T parseSnapshot(DataSnapshot snapshot) {
        return snapshot.getValue(mModelClass);
    }

    public DatabaseReference getRef(int position) {
        return mSnapshots.getItem(position).getRef();
    }

    @Override
    public long getItemId(int i) {
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        return mSnapshots.getItem(i).getKey().hashCode();
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(mLayout, viewGroup, false);
        }

        T model = getItem(position);

        // Call out to subclass to marshall this model into the provided view
        populateView(view, model, position);
        return view;
    }

    /**
     * @see ChangeEventListener#onChildChanged(ChangeEventListener.EventType, int, int)
     */
    protected void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
        notifyDataSetChanged();
    }

    /**
     * @see ChangeEventListener#onDataChanged()
     */
    protected void onDataChanged() {
    }

    /**
     * @see ChangeEventListener#onCancelled(DatabaseError)
     */
    protected void onCancelled(DatabaseError error) {
        Log.w(TAG, error.toException());
    }

    /**
     * Each time the data at the given Firebase location changes,
     * this method will be called for each item that needs to be displayed.
     * The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     *
     * @param v        The view to populate
     * @param model    The object containing the data used to populate the view
     * @param position The position in the list of the view being populated
     */
    protected abstract void populateView(View v, T model, int position);
}
