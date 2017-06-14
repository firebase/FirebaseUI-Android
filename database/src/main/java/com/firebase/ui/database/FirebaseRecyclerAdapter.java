package com.firebase.ui.database;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This class is a generic way of backing a {@link RecyclerView} with a Firebase location. It
 * handles all of the child events at the given Firebase location and marshals received data into
 * the given class type.
 * <p>
 * See the <a href="https://github.com/firebase/FirebaseUI-Android/blob/master/database/README.md">README</a>
 * for an in-depth tutorial on how to set up the FirebaseRecyclerAdapter.
 *
 * @param <T>  The Java class that maps to the type of objects stored in the Firebase location.
 * @param <VH> The {@link RecyclerView.ViewHolder} class that contains the Views in the layout that
 *             is shown for each object.
 */
public abstract class FirebaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements FirebaseAdapter<T> {
    private static final String TAG = "FirebaseRecyclerAdapter";

    protected final ObservableSnapshotArray<T> mSnapshots;
    protected final Class<VH> mViewHolderClass;
    protected final int mModelLayout;

    /**
     * @param snapshots       The data used to populate the adapter
     * @param modelLayout     This is the layout used to represent a single item in the list. You
     *                        will be responsible for populating an instance of the corresponding
     *                        view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance
     *                        modelLayout.
     */
    public FirebaseRecyclerAdapter(ObservableSnapshotArray<T> snapshots,
                                   @LayoutRes int modelLayout,
                                   Class<VH> viewHolderClass) {
        mSnapshots = snapshots;
        mViewHolderClass = viewHolderClass;
        mModelLayout = modelLayout;

        startListening();
    }

    /**
     * @param parser a custom {@link SnapshotParser} to convert a {@link DataSnapshot} to the model
     *               class
     * @param query  The Firebase location to watch for data changes. Can also be a slice of a
     *               location, using some combination of {@code limit()}, {@code startAt()}, and
     *               {@code endAt()}. <b>Note, this can also be a {@link DatabaseReference}.</b>
     * @see #FirebaseRecyclerAdapter(ObservableSnapshotArray, int, Class)
     */
    public FirebaseRecyclerAdapter(SnapshotParser<T> parser,
                                   @LayoutRes int modelLayout,
                                   Class<VH> viewHolderClass,
                                   Query query) {
        this(new FirebaseArray<>(query, parser), modelLayout, viewHolderClass);
    }

    /**
     * @see #FirebaseRecyclerAdapter(SnapshotParser, int, Class, Query)
     */
    public FirebaseRecyclerAdapter(Class<T> modelClass,
                                   @LayoutRes int modelLayout,
                                   Class<VH> viewHolderClass,
                                   Query query) {
        this(new ClassSnapshotParser<>(modelClass), modelLayout, viewHolderClass, query);
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
        switch (type) {
            case ADDED:
                notifyItemInserted(index);
                break;
            case CHANGED:
                notifyItemChanged(index);
                break;
            case REMOVED:
                notifyItemRemoved(index);
                break;
            case MOVED:
                notifyItemMoved(oldIndex, index);
                break;
            default:
                throw new IllegalStateException("Incomplete case statement");
        }
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
    public int getItemCount() {
        return mSnapshots.size();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        try {
            Constructor<VH> constructor = mViewHolderClass.getConstructor(View.class);
            return constructor.newInstance(view);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mModelLayout;
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        T model = getItem(position);
        populateViewHolder(viewHolder, model, position);
    }

    /**
     * Each time the data at the given Firebase location changes,
     * this method will be called for each item that needs to be displayed.
     * The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     *
     * @param viewHolder The view to populate
     * @param model      The object containing the data used to populate the view
     * @param position   The position in the list of the view being populated
     */
    protected abstract void populateViewHolder(VH viewHolder, T model, int position);
}
