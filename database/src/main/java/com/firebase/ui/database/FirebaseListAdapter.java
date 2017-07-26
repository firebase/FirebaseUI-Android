package com.firebase.ui.database;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
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

    protected final Context mContext;
    protected final ObservableSnapshotArray<T> mSnapshots;
    protected final int mLayout;

    /**
     * @param context     The {@link Activity} containing the {@link ListView}
     * @param snapshots   The data used to populate the adapter
     * @param modelLayout This is the layout used to represent a single list item. You will be
     *                    responsible for populating an instance of the corresponding view with the
     *                    data from an instance of modelClass.
     * @param owner       the lifecycle owner used to automatically listen and cleanup after {@link
     *                    FragmentActivity#onStart()} and {@link FragmentActivity#onStop()} events
     *                    reflectively.
     */
    public FirebaseListAdapter(Context context,
                               ObservableSnapshotArray<T> snapshots,
                               @LayoutRes int modelLayout,
                               LifecycleOwner owner) {
        mContext = context;
        mSnapshots = snapshots;
        mLayout = modelLayout;

        if (owner != null) { owner.getLifecycle().addObserver(this); }
    }

    /**
     * @see FirebaseListAdapter#FirebaseListAdapter(Context, ObservableSnapshotArray, int,
     * LifecycleOwner)
     */
    public FirebaseListAdapter(Context context,
                               ObservableSnapshotArray<T> snapshots,
                               @LayoutRes int modelLayout) {
        this(context, snapshots, modelLayout, null);
        startListening();
    }

    /**
     * @param parser a custom {@link SnapshotParser} to convert a {@link DataSnapshot} to the model
     *               class
     * @param query  The Firebase location to watch for data changes. Can also be a slice of a
     *               location, using some combination of {@code limit()}, {@code startAt()}, and
     *               {@code endAt()}. <b>Note, this can also be a {@link DatabaseReference}.</b>
     * @see #FirebaseListAdapter(Context, ObservableSnapshotArray, int)
     */
    public FirebaseListAdapter(Context context,
                               SnapshotParser<T> parser,
                               @LayoutRes int modelLayout,
                               Query query) {
        this(context, new FirebaseArray<>(query, parser), modelLayout);
    }

    /**
     * @see #FirebaseListAdapter(Context, SnapshotParser, int, Query)
     */
    public FirebaseListAdapter(Context context,
                               Class<T> modelClass,
                               @LayoutRes int modelLayout,
                               Query query) {
        this(context, new ClassSnapshotParser<>(modelClass), modelLayout, query);
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (!mSnapshots.isListening(this)) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    @Override
    public void cleanup() {
        mSnapshots.removeChangeEventListener(this);
    }

    @SuppressWarnings("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    void cleanup(LifecycleOwner source, Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_STOP) {
            cleanup();
        } else if (event == Lifecycle.Event.ON_DESTROY) {
            source.getLifecycle().removeObserver(this);
        }
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
            view = LayoutInflater.from(mContext).inflate(mLayout, viewGroup, false);
        }

        T model = getItem(position);

        // Call out to subclass to marshall this model into the provided view
        populateView(view, model, position);
        return view;
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
    protected abstract void populateView(View v, T model, int position);
}
