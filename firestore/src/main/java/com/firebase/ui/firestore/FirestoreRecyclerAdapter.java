package com.firebase.ui.firestore;

import android.support.v7.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

// TODO: Document
public abstract class FirestoreRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ChangeEventListener {

    private Class<T> mModelClass;
    private FirestoreArray<T> mArray;

    public FirestoreRecyclerAdapter(Query query, Class<T> modelClass) {
        mModelClass = modelClass;
        mArray = new FirestoreArray<T>(query, modelClass);
    }

    public abstract void onBindViewHolder(VH vh, int i, T model);

    public void startListening() {
        mArray.addChangeEventListener(this);
    }

    public void stopListening() {
        mArray.removeChangeEventListener(this);
    }

    @Override
    public void onBindViewHolder(VH vh, int i) {
        T model = mArray.getObject(i);
        onBindViewHolder(vh, i, model);
    }

    @Override
    public int getItemCount() {
        return mArray.size();
    }

    @Override
    public void onChildChanged(DocumentChange.Type eventType,
                               DocumentSnapshot snapshot,
                               int index) {
        switch (eventType) {
            case ADDED:
                // TODO: Why doesn't this work
//                notifyItemInserted(index);
                notifyDataSetChanged();
                break;
            case REMOVED:
                notifyItemRemoved(index);
                break;
            case MODIFIED:
                notifyItemChanged(index);
                break;
        }
    }

    @Override
    public void onDataChanged() {
        // No-op
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        // No-op
    }
}
