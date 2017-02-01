package com.firebase.ui.database;

import android.util.Pair;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Acts as a bridge between a list of {@link DataSnapshot}s and a list of objects of type E.
 *
 * @param <E> the model representation of a {@link DataSnapshot}
 */
public class FirebaseArrayOfObjects<E> extends ImmutableList<E> {
    protected List<DataSnapshot> mSnapshots;
    protected Class<E> mEClass;
    protected SnapshotParser<E> mParser;

    /**
     * @param snapshots  a list of {@link DataSnapshot}s to be converted to a model type
     * @param modelClass the model representation of a {@link DataSnapshot}
     */
    protected FirebaseArrayOfObjects(List<DataSnapshot> snapshots, Class<E> modelClass) {
        mSnapshots = snapshots;
        mEClass = modelClass;
        mParser = new SnapshotParser<E>() {
            @Override
            public E parseSnapshot(DataSnapshot snapshot) {
                return snapshot.getValue(mEClass);
            }
        };
    }

    /**
     * @param snapshots  a list of {@link DataSnapshot}s to be converted to a model type
     * @param modelClass the model representation of a {@link DataSnapshot}
     */
    public static <T> FirebaseArrayOfObjects<T> newInstance(List<DataSnapshot> snapshots,
                                                            Class<T> modelClass) {
        if (snapshots instanceof FirebaseArray) {
            return new FirebaseArrayOfObjectsOptimized<>((FirebaseArray) snapshots, modelClass);
        } else {
            return new FirebaseArrayOfObjects<>(snapshots, modelClass);
        }
    }

    /**
     * @param parser a custom {@link SnapshotParser} to manually convert each {@link DataSnapshot}
     *               to its model type
     * @see #newInstance(List, Class)
     */
    public static <T> FirebaseArrayOfObjects<T> newInstance(List<DataSnapshot> snapshots,
                                                            Class<T> modelClass,
                                                            SnapshotParser<T> parser) {
        FirebaseArrayOfObjects<T> array = newInstance(snapshots, modelClass);
        array.mParser = parser;
        return array;
    }

    public List<DataSnapshot> getSnapshots() {
        return mSnapshots;
    }

    protected List<E> getObjects() {
        List<E> objects = new ArrayList<>(mSnapshots.size());
        for (int i = 0; i < mSnapshots.size(); i++) {
            objects.add(get(i));
        }
        return objects;
    }

    @Override
    public int size() {
        return mSnapshots.size();
    }

    @Override
    public boolean isEmpty() {
        return mSnapshots.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable iterator
     */
    @Override
    public Iterator<E> iterator() {
        return new ImmutableIterator(getObjects().iterator());
    }

    @Override
    public Object[] toArray() {
        return getObjects().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getObjects().toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getObjects().containsAll(c);
    }

    @Override
    public E get(int index) {
        return mParser.parseSnapshot(mSnapshots.get(index));
    }

    @Override
    public int indexOf(Object o) {
        return getObjects().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return getObjects().lastIndexOf(o);
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable list iterator
     */
    @Override
    public ListIterator<E> listIterator() {
        return new ImmutableListIterator(getObjects().listIterator());
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable list iterator
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        return new ImmutableListIterator(getObjects().listIterator(index));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FirebaseArrayOfObjects<?> array = (FirebaseArrayOfObjects<?>) obj;

        return mSnapshots.equals(array.mSnapshots) && mEClass.equals(array.mEClass);
    }

    @Override
    public int hashCode() {
        int result = mSnapshots.hashCode();
        result = 31 * result + mEClass.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FirebaseArrayOfObjects{" +
                "mSnapshots=" + mSnapshots +
                '}';
    }

    protected static class FirebaseArrayOfObjectsOptimized<E> extends FirebaseArrayOfObjects<E>
            implements ChangeEventListener, SubscriptionEventListener {
        protected List<E> mObjects = new ArrayList<>();
        protected Pair<Boolean, Boolean> mIsListening$AddedListener = new Pair<>(true, false);

        public FirebaseArrayOfObjectsOptimized(FirebaseArray snapshots, Class<E> modelClass) {
            super(snapshots, modelClass);
            snapshots.addChangeEventListener(this);
            snapshots.addSubscriptionEventListener(this);
        }

        @Override
        protected List<E> getObjects() {
            return mObjects;
        }

        @Override
        public void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
            switch (type) {
                case ADDED:
                    mObjects.add(get(index));
                    break;
                case CHANGED:
                    mObjects.set(index, get(index));
                    break;
                case REMOVED:
                    mObjects.remove(index);
                    break;
                case MOVED:
                    mObjects.add(index, mObjects.remove(oldIndex));
                    break;
            }
        }

        @Override
        public void onSubscriptionRemoved() {
            FirebaseArray snapshots = (FirebaseArray) mSnapshots;
            if (!snapshots.isListening()) {
                snapshots.removeChangeEventListener(this);
                mIsListening$AddedListener = new Pair<>(false, false);
            }
        }

        @Override
        public void onSubscriptionAdded() {
            if (mIsListening$AddedListener.second) {
                mIsListening$AddedListener = new Pair<>(true, false);
            } else if (!mIsListening$AddedListener.first) {
                ((FirebaseArray) mSnapshots).addChangeEventListener(this);
                mIsListening$AddedListener = new Pair<>(true, true);
            }
        }

        @Override
        public void onDataChanged() {
        }

        @Override
        public void onCancelled(DatabaseError error) {
        }
    }
}
