package com.firebase.ui.database;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Acts as a bridge between a list of {@link DataSnapshot}s and a list of objects of type E.
 *
 * @param <E> the object representation of a {@link DataSnapshot}
 */
public class FirebaseObjectsArray<E> extends UnmodifiableList<E> {
    private List<DataSnapshot> mSnapshots;
    private Class<E> mEClass;

    public FirebaseObjectsArray(List<DataSnapshot> snapshots, Class<E> eClass) {
        mSnapshots = snapshots;
        mEClass = eClass;
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
        return mSnapshots.get(index).getValue(mEClass);
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

        FirebaseObjectsArray<?> array = (FirebaseObjectsArray<?>) obj;

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
        return "FirebaseObjectsArray{" +
                "mSnapshots=" + mSnapshots +
                '}';
    }
}
