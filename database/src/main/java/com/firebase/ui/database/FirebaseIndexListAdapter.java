package com.firebase.ui.database;

import android.app.Activity;
import android.support.annotation.LayoutRes;

import com.google.firebase.database.Query;

/**
 * This class is a generic way of backing an Android ListView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type. Extend this class and provide an implementation of {@code populateView}, which will be given an
 * instance of your list item mLayout and an instance your class that holds your data. Simply populate the view however
 * you like and this class will handle updating the list as the data changes.
 * <p>
 * If your data is not indexed:
 * <pre>
 *     DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
 *     ListAdapter adapter = new FirebaseListAdapter<ChatMessage>(this, ChatMessage.class, android.R.layout.two_line_list_item, keyRef, dataRef)
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
 * @param <T> The class type to use as a model for the data contained in the children of the given Firebase location
 */
public abstract class FirebaseIndexListAdapter<T> extends FirebaseListAdapter<T> {
    /**
     * @param activity    The activity containing the ListView
     * @param modelClass  Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout This is the layout used to represent a single list item. You will be responsible for populating an
     *                    instance of the corresponding view with the data from an instance of modelClass.
     * @param keyRef      The Firebase location containing the list of keys to be found in {@code dataRef}.
     *                    Can also be a slice of a location, using some
     *                    combination of {@code limit()}, {@code startAt()}, and {@code endAt()}.
     * @param dataRef     The Firebase location to watch for data changes.
     *                    Each key key found in {@code keyRef}'s location represents a list item in the {@code ListView}.
     */
    public FirebaseIndexListAdapter(Activity activity,
                                    Class<T> modelClass,
                                    @LayoutRes int modelLayout,
                                    Query keyRef,
                                    Query dataRef) {
        super(activity, modelClass, modelLayout, new FirebaseIndexArray(keyRef, dataRef));
    }
}
