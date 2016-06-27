package com.firebase.ui.database;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

/**
 * This class is a generic way of backing an Android {@link FragmentStatePagerAdapter} with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type. Extend this class and provide an implementation of <code>instantiateFragment</code>, which will be given an
 * instance of your class that holds your data and returns the fragment for the Pager. Simply instantiate the Fragment
 * you like and this class will handle updating the pager as the data changes.
 *
 * <blockquote><pre>
 * {@code
 *     DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
 *     FragmentStatePagerAdapter adapter = new FirebaseFragmentStatePagerAdapter<Object>(getChildFragmentManager(), Object.class, mRef)
 *     {
 *         protected Fragment instantiateFragment(Object object, int position)
 *         {
 *             return Fragment.newInstance(object, position);
 *         }
 *     };
 *     viewPager.setAdapter(adapter);
 * }
 * </pre></blockquote>
 *
 * @param <T> The class type to use as a model for the data contained in the children of the given Firebase location
 * @param <F> The Fragment class type you want to instantiate
 */
public abstract class FirebaseFragmentStatePagerAdapter<T, F extends Fragment> extends FragmentStatePagerAdapter {
    private final Class<T> mModelClass;
    FirebaseArray mSnapshots;

    /**
     * @param fragmentManager   A fragment manager. please use the child fragment manager when you using this class inside a fragment
     * @param modelClass        Firebase will marshall the data at a location into an instance of a class that you provide
     * @param ref               The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                          combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>,
     */
    public FirebaseFragmentStatePagerAdapter(FragmentManager fragmentManager, Class<T> modelClass, Query ref) {
        super(fragmentManager);

        mModelClass = modelClass;
        mSnapshots = new FirebaseArray(ref);
        mSnapshots.setOnChangedListener(new FirebaseArray.OnChangedListener() {
            @Override
            public void onChanged(EventType type, int index, int oldIndex) {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * @param fragmentManager   A fragment manager. please use the child fragment manager when you using this class inside a fragment
     * @param modelClass        Firebase will marshall the data at a location into an instance of a class that you provide
     * @param ref               The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                          combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>,
     */
    public FirebaseFragmentStatePagerAdapter(FragmentManager fragmentManager, Class<T> modelClass, DatabaseReference ref) {
        this(fragmentManager, modelClass, (Query) ref);
    }

    public void cleanUp() {
        // We're being destroyed, let go of our mListener and forget about all of the mModels
        mSnapshots.cleanup();
    }

    @Override
    public F getItem(int position) {
        T item = parseSnapshot(mSnapshots.getItem(position));

        return instantiateFragment(item, position);
    }

    @Override
    public int getCount() {
        return mSnapshots.getCount();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
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

    public DatabaseReference getRef(int position) { return mSnapshots.getItem(position).getRef(); }

    /**
     * Each time the data at the given Firebase location changes, this method will be called for each item that needs
     * to be displayed. The first argument corresponds to the mModelClass given to the constructor of
     * this class. The second argument is the item's position in the adapter.
     * <p>
     * Your implementation should return a fragment using the data contained in the model.
     *
     * @param model     The object containing the data used to populate the view
     * @param position  The position in the list of the view being populated
     */
    abstract protected F instantiateFragment(T model, int position);
}