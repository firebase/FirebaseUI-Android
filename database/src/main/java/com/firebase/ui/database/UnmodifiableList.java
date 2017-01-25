package com.firebase.ui.database;

import android.support.annotation.RestrictTo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class UnmodifiableList<E> implements List<E> {
    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean add(E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    protected static class ImmutableIterator<T> implements Iterator<T> {
        private Iterator<T> mIterator;

        public ImmutableIterator(Iterator<T> iterator) {
            mIterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return mIterator.hasNext();
        }

        @Override
        public T next() {
            return mIterator.next();
        }
    }

    protected static class ImmutableListIterator<T> implements ListIterator<T> {
        private ListIterator<T> mListIterator;

        public ImmutableListIterator(ListIterator<T> listIterator) {
            mListIterator = listIterator;
        }

        @Override
        public boolean hasNext() {
            return mListIterator.hasNext();
        }

        @Override
        public T next() {
            return mListIterator.next();
        }

        @Override
        public boolean hasPrevious() {
            return mListIterator.hasPrevious();
        }

        @Override
        public T previous() {
            return mListIterator.previous();
        }

        @Override
        public int nextIndex() {
            return mListIterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return mListIterator.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();
        }
    }
}
