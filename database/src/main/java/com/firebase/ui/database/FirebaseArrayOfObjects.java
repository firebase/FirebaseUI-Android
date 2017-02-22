package com.firebase.ui.database;

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
    protected FirebaseArrayOfObjects(List<DataSnapshot> snapshots,
                                     Class<E> modelClass,
                                     SnapshotParser<E> parser) {
        mSnapshots = snapshots;
        mEClass = modelClass;
        mParser = parser;
    }

    /**
     * @param snapshots  a list of {@link DataSnapshot}s to be converted to a model type
     * @param modelClass the model representation of a {@link DataSnapshot}
     */
    public static <T> FirebaseArrayOfObjects<T> newInstance(List<DataSnapshot> snapshots,
                                                            final Class<T> modelClass) {
        return getArray(snapshots, modelClass, new SnapshotParser<T>() {
            @Override
            public T parseSnapshot(DataSnapshot snapshot) {
                return snapshot.getValue(modelClass);
            }
        });
    }

    /**
     * @param parser a custom {@link SnapshotParser} to manually convert each {@link DataSnapshot}
     *               to its model type
     * @see #newInstance(List, Class)
     */
    public static <T> FirebaseArrayOfObjects<T> newInstance(List<DataSnapshot> snapshots,
                                                            Class<T> modelClass,
                                                            SnapshotParser<T> parser) {
        return getArray(snapshots, modelClass, parser);
    }

    private static <T> FirebaseArrayOfObjects<T> getArray(List<DataSnapshot> snapshots,
                                                          Class<T> modelClass,
                                                          SnapshotParser<T> parser) {
        if (snapshots instanceof FirebaseArray) {
            return new Optimized<>((FirebaseArray) snapshots, modelClass, parser);
        } else {
            return new FirebaseArrayOfObjects<>(snapshots, modelClass, parser);
        }
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
        return mSnapshots.toString();
    }

    protected static class Optimized<E> extends FirebaseArrayOfObjects<E> implements ChangeEventListener {
        protected List<E> mObjects = new ArrayList<>();

        public Optimized(FirebaseArray snapshots, Class<E> modelClass, SnapshotParser<E> parser) {
            super(snapshots, modelClass, parser);
            snapshots.addChangeEventListener(this);
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
        public void onDataChanged() {
        }

        @Override
        public void onCancelled(DatabaseError error) {
        }
    }
}
