/*
 * Firebase UI Bindings Android Library
 *
 * Copyright © 2015 Firebase - All Rights Reserved
 * https://www.firebase.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY FIREBASE AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL FIREBASE BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.firebase.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This class is a generic way of backing an RecyclerView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type.
 *
 * To use this class in your app, subclass it passing in all required parameters and implement the
 * populateViewHolder method.
 *
 * <blockquote><pre>
 * {@code
 *     private static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
 *         TextView messageText;
 *         TextView nameText;
 *
 *         public ChatMessageViewHolder(View itemView) {
 *             super(itemView);
 *             nameText = (TextView)itemView.findViewById(android.R.id.text1);
 *             messageText = (TextView) itemView.findViewById(android.R.id.text2);
 *         }
 *     }
 *
 *     FirebaseRecyclerViewAdapter<ChatMessage, ChatMessageViewHolder> adapter;
 *     ref = new Firebase("https://<yourapp>.firebaseio.com");
 *
 *     RecyclerView recycler = (RecyclerView) findViewById(R.id.messages_recycler);
 *     recycler.setHasFixedSize(true);
 *     recycler.setLayoutManager(new LinearLayoutManager(this));
 *
 *     adapter = new FirebaseRecyclerViewAdapter<ChatMessage, ChatMessageViewHolder>(ChatMessage.class, android.R.layout.two_line_list_item, ChatMessageViewHolder.class, mRef) {
 *         public void populateViewHolder(ChatMessageViewHolder chatMessageViewHolder, ChatMessage chatMessage) {
 *             chatMessageViewHolder.nameText.setText(chatMessage.getName());
 *             chatMessageViewHolder.messageText.setText(chatMessage.getMessage());
 *         }
 *     };
 *     recycler.setAdapter(mAdapter);
 * }
 * </pre></blockquote>
 *
 * @param <T> The Java class that maps to the type of objects stored in the Firebase location.
 * @param <VH> The ViewHolder class that contains the Views in the layout that is shown for each object.
 */
public abstract class FirebaseRecyclerViewAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    Class<T> mModelClass;
    protected int mModelLayout;
    Class<VH> mViewHolderClass;
    FirebaseArray mSnapshots;

    /**
     * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                    instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref        The Firebase location to watch for data changes.
     * @param pageSize   initial page size. set 0 for no limit.
     */
    public FirebaseRecyclerViewAdapter(Class<T> modelClass, int modelLayout, Class<VH> viewHolderClass, Query ref, int pageSize, boolean orderASC) {
        mModelClass = modelClass;
        mModelLayout = modelLayout;
        mViewHolderClass = viewHolderClass;
        mSnapshots = new FirebaseArray(ref, pageSize, orderASC);

        mSnapshots.setOnChangedListener(new FirebaseArray.OnChangedListener() {
            @Override
            public void onChanged(EventType type, int index, int oldIndex) {
                switch (type) {
                    case Added:
                        notifyItemInserted(index + getSnapShotOffset());
                        break;
                    case Changed:
                        notifyItemChanged(index + getSnapShotOffset());
                        break;
                    case Removed:
                        notifyItemRemoved(index + getSnapShotOffset());
                        break;
                    case Moved:
                        notifyItemMoved(oldIndex + getSnapShotOffset(), index + getSnapShotOffset());
                        break;
                    case Reset:
                        notifyDataSetChanged();
                        break;
                    default:
                        throw new IllegalStateException("Incomplete case statement");
                }
            }
        });

        mSnapshots.setOnSyncStatusChangedListener(new FirebaseArray.OnSyncStatusChangedListener() {
            @Override
            public void onChanged(EventType type) {
                onSyncStatusChanged(type == EventType.Synced);
            }
        });

        mSnapshots.setOnErrorListener(new FirebaseArray.OnErrorListener() {
            @Override
            public void onError(FirebaseError firebaseError) {
                onArrayError(firebaseError);
            }
        });
    }

    /**
     * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                    instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref        The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                   combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>
     */
    public FirebaseRecyclerViewAdapter(Class<T> modelClass, int modelLayout, Class<VH> viewHolderClass, Query ref) {
        this(modelClass, modelLayout, viewHolderClass, ref, 0, true);
    }

    /**
     * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                    instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref        The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                   combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>
     */
    public FirebaseRecyclerViewAdapter(Class<T> modelClass, int modelLayout, Class<VH> viewHolderClass, Firebase ref) {
        this(modelClass, modelLayout, viewHolderClass, (Query) ref);
    }

    /**
     * Increase the limit of the query by one page.
     */
    public void more() {
        mSnapshots.more();
    }

    /**
     * Reset the limit of the query to its original size.
     */
    public void reset() {
        mSnapshots.reset();
    }

    public void cleanup() {
        mSnapshots.cleanup();
    }

    /**
     * Override when adding headers.
     * @return number of items inserted in front of the FirebaseArray
     */
    public int getSnapShotOffset() {
        return 0;
    }

    /**
     * Override when adding headers or footers
     * @return number of items including headers and footers.
     */
    @Override
    public int getItemCount() {
        return mSnapshots.getCount();
    }

    public T getItem(int position) {
        return mSnapshots.getItem(position - getSnapShotOffset()).getValue(mModelClass);
    }

    public Firebase getRef(int position) { return mSnapshots.getItem(position).getRef(); }

    @Override
    public long getItemId(int position) {
        if(position < getSnapShotOffset()) return ("header" + position).hashCode();
        if(position >= getSnapShotOffset() + mSnapshots.getCount()) return ("footer" + (position - (getSnapShotOffset() + mSnapshots.getCount()))).hashCode();
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        return mSnapshots.getItem(position).getKey().hashCode();
    }

    /**
     * Override when adding headers or footers.
     */
    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(mModelLayout, parent, false);
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
    public void onBindViewHolder(VH viewHolder, int position) {
        T model = null;
        int arrayPosition = position - getSnapShotOffset();
        if(arrayPosition < mSnapshots.getCount() && arrayPosition >= 0) {
            model = getItem(position);
        }
        populateViewHolder(viewHolder, model, position);
    }

    /**
     * Each time the data at the given Firebase location changes, this method will be called for each item that needs
     * to be displayed. The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     * You should implement either this method or the other FirebaseRecyclerViewAdapter#populateViewHolder(VH, Object) method
     * but not both.
     *
     * @param viewHolder The view to populate
     * @param model      The object containing the data used to populate the view
     * @param position  The position in the list of the view being populated
     */
    protected void populateViewHolder(VH viewHolder, T model, int position) {
        populateViewHolder(viewHolder, model);
    }
    /**
     * This is a backwards compatible version of populateViewHolder.
     * <p>
     * You should implement either this method or the other FirebaseRecyclerViewAdapter#populateViewHolder(VH, T, int) method
     * but not both.
     *
     * @see FirebaseListAdapter#populateView(View, Object, int)
     *
     * @param viewHolder The view to populate
     * @param model      The object containing the data used to populate the view
     */
    protected void populateViewHolder(VH viewHolder, T model) {
    }

    protected void onSyncStatusChanged(boolean synced) {}
    protected void onArrayError(FirebaseError firebaseError) {}
}
