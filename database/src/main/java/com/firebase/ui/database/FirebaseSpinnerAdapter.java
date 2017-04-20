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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/**
 * This class is a generic way of backing an Android ListView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type. Extend this class and provide an implementation of <code>populateView</code> and <code>populateDropdownView</code>,
 * which will be given an instance of your list item mLayout, your drowndown list item mDropdownLayout, and an instance your
 * class that holds your data. Simply populate the views however you like and this class will handle updating the list
 * as the data changes.
 *
 * <blockquote><pre>
 * {@code
 *     Spinner spinner = (Spinner) findViewById(R.id.spinner);
 *
 *     DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
 *     SpinnerAdapter adapter = new FirebaseSpinnerAdapter<ChatMessage>(this, ChatMessage.class, android.R.layout.simple_spinner_item,
 *              android.R.layout.simple_spinner_dropdown_item, ref) {
 *
 *         protected void populateView(View view, ChatMessage chatMessage, int position) {
 *             ((TextView) view.findViewById(android.R.id.text1)).setText(chatMessage.getName());
 *         }
 *
 *         protected void populateDropdownView(View view, ChatMessage chatMessage, int position) {
 *              // Maybe style the view or dynamically change dropdown view
 *             ((TextView) view.findViewById(android.R.id.text1)).setText(chatMessage.getName());
 *         }
 *     };
 *     spinner.setAdapter(adapter);
 * }
 * </pre></blockquote>
 *
 * @param <T> The class type to use as a model for the data contained in the children of the given Firebase location
 */
public abstract class FirebaseSpinnerAdapter<T> extends FirebaseListAdapter<T> implements
        SpinnerAdapter {

    private static final String TAG = "FirebaseSpinnerAdapter";

    // Other variables already handled in superclass
    protected int mDropDownLayout;
    protected Activity mActivity;

    /**
     * @param activity The activity containing the ListView
     * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
     * @param layout This is the layout used to represent a single list item. You will be responsible for populating an
     *             instance of the corresponding view with the data from an instance of modelClass.
     * @param query The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *              combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>,
     */
    public FirebaseSpinnerAdapter(Activity activity, Class<T> modelClass, int layout,
                                  int dropdownLayout, Query query) {
        super(activity, modelClass, layout, query);
        mDropDownLayout = dropdownLayout;
    }

    /**
     * @param activity The activity containing the ListView
     * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
     * @param layout This is the layout used to represent a single list item. You will be responsible for populating an
     *             instance of the corresponding view with the data from an instance of modelClass.
     * @param reference The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                  combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>,
     */
    public FirebaseSpinnerAdapter(Activity activity, Class<T> modelClass, int layout,
                                  int dropdownLayout, DatabaseReference reference) {
        super(activity, modelClass, layout, reference);
        mDropDownLayout = dropdownLayout;
    }

    @Override
    protected void onCancelled(DatabaseError databaseError) {
        Log.w(TAG, databaseError.toException());
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mActivity.getLayoutInflater().inflate(mDropDownLayout, parent, false);
        }

        T model = getItem(position);

        populateDropdownView(convertView, model, position);
        return convertView;
    }

    /**
     * Each time the data at the given Firebase location changes, this method will be called for each item that needs
     * to be displayed. The first two arguments correspond to the mDropdownLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the dropdown view using the data contained in the model.
     *
     * @param dropdownView The spinner's dropdown view to populate
     * @param model The object containing the data used to populate the view
     * @param position The position in the list of the view being populated
     */
    abstract protected void populateDropdownView(View dropdownView, T model, int position);
}
