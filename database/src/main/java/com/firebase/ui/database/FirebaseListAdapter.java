package com.firebase.ui.database;

import android.app.Activity;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

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
public abstract class FirebaseListAdapter<T> extends BaseAdapter implements FirebaseAdapter<T> {
    private static final String TAG = "FirebaseListAdapter";

    protected Activity mActivity;
    protected ObservableSnapshotArray<T> mSnapshots;
    protected int mLayout;

    /**
     * @param activity    The {@link Activity} containing the {@link ListView}
     * @param modelLayout This is the layout used to represent a single list item. You will be
     *                    responsible for populating an instance of the corresponding view with the
     *                    data from an instance of modelClass.
     * @param snapshots   The data used to populate the adapter
     */
    public FirebaseListAdapter(Activity activity,
                               ObservableSnapshotArray<T> snapshots,
                               @LayoutRes int modelLayout) {
        mActivity = activity;
        mSnapshots = snapshots;
        mLayout = modelLayout;

        startListening();
    }

    /**
     * @param parser a custom {@link SnapshotParser} to convert a {@link DataSnapshot} to the model
     *               class
     * @param query  The Firebase location to watch for data changes. Can also be a slice of a
     *               location, using some combination of {@code limit()}, {@code startAt()}, and
     *               {@code endAt()}.
     * @see #FirebaseListAdapter(Activity, ObservableSnapshotArray, int)
     */
    public FirebaseListAdapter(Activity activity,
                               SnapshotParser<T> parser,
                               @LayoutRes int modelLayout,
                               Query query) {
        this(activity, new FirebaseArray<>(query, parser), modelLayout);
    }

    /**
     * @see #FirebaseListAdapter(Activity, SnapshotParser, int, Query)
     */
    public FirebaseListAdapter(Activity activity,
                               Class<T> modelClass,
                               @LayoutRes int modelLayout,
                               Query query) {
        this(activity, new ClassSnapshotParser<>(modelClass), modelLayout, query);
    }

    @Override
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    @Override
    public void cleanup() {
        mSnapshots.removeChangeEventListener(this);
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type,
                               DataSnapshot snapshot,
                               int index,
                               int oldIndex) {
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
        return mSnapshots.getObject(position);
    }

    @Override
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
