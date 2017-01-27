package com.firebase.ui.database.adapter;

import android.app.Activity;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

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
public abstract class FirebaseListAdapter<T> extends BaseAdapter implements ChangeEventListener, SnapshotParser<T> {
    private static final String TAG = "FirebaseListAdapter";

    protected Activity mActivity;
    protected FirebaseArray mSnapshots;
    protected Class<T> mModelClass;
    protected int mLayout;

    /**
     * @param activity    The {@link Activity} containing the {@link ListView}
     * @param modelClass  Firebase will marshall the data at a location into an instance of a class
     *                    that you provide
     * @param modelLayout This is the layout used to represent a single list item. You will be
     *                    responsible for populating an instance of the corresponding view with the
     *                    data from an instance of modelClass.
     * @param snapshots   The data used to populate the adapter
     */
    public FirebaseListAdapter(Activity activity,
                               FirebaseArray snapshots,
                               Class<T> modelClass,
                               @LayoutRes int modelLayout) {
        mActivity = activity;
        mSnapshots = snapshots;
        mModelClass = modelClass;
        mLayout = modelLayout;

        startListening();
    }

    /**
     * @param ref The Firebase location to watch for data changes. Can also be a slice of a
     *            location, using some combination of {@code limit()}, {@code startAt()}, and {@code
     *            endAt()}.
     * @see #FirebaseListAdapter(Activity, FirebaseArray, Class, int)
     */
    public FirebaseListAdapter(Activity activity,
                               Class<T> modelClass,
                               @LayoutRes int modelLayout,
                               Query ref) {
        this(activity, new FirebaseArray(ref), modelClass, modelLayout);
    }

    /**
     * If you need to do some setup before we start listening for change events in the database
     * (such as setting a custom {@link JoinResolver}), do so it here and then call {@code
     * super.startListening()}.
     */
    protected void startListening() {
        if (!mSnapshots.isListening()) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    public void cleanup() {
        mSnapshots.removeChangeEventListener(this);
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
        notifyDataSetChanged();
    }

    @Override
    public void onDataChanged() {
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.w(TAG, error.toException());
    }

    @Override
    public T getItem(int position) {
        return parseSnapshot(mSnapshots.get(position));
    }

    @Override
    public T parseSnapshot(DataSnapshot snapshot) {
        return snapshot.getValue(mModelClass);
    }

    public DatabaseReference getRef(int position) {
        return mSnapshots.get(position).getRef();
    }

    @Override
    public int getCount() {
        return mSnapshots.size();
    }

    @Override
    public long getItemId(int i) {
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        return mSnapshots.get(i).getKey().hashCode();
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
