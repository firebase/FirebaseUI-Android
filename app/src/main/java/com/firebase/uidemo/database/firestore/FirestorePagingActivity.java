package com.firebase.uidemo.database.firestore;

import android.arch.paging.PagedList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.firebase.uidemo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FirestorePagingActivity extends AppCompatActivity {

    private static final String TAG = "FirestorePagingActivity";

    @BindView(R.id.paging_recycler)
    RecyclerView mRecycler;

    @BindView(R.id.paging_loading)
    ProgressBar mProgressBar;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItemsCollection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firestore_paging);
        ButterKnife.bind(this);

        mFirestore = FirebaseFirestore.getInstance();
        mItemsCollection = mFirestore.collection("items");

        setUpAdapter();
    }

    private void setUpAdapter() {
        Query baseQuery = mItemsCollection.orderBy("value", Query.Direction.ASCENDING);

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .setPageSize(20)
                .build();

        FirestorePagingOptions<Item> options = new FirestorePagingOptions.Builder<Item>()
                .setLifecycleOwner(this)
                .setQuery(baseQuery, config, Item.class)
                .build();

        FirestorePagingAdapter<Item, ItemViewHolder> adapter =
                new FirestorePagingAdapter<Item, ItemViewHolder>(options) {
                    @NonNull
                    @Override
                    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                             int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_item, parent, false);
                        return new ItemViewHolder(view);
                    }

                    @Override
                    protected void onBindViewHolder(@NonNull ItemViewHolder holder,
                                                    int position,
                                                    @NonNull Item model) {
                        holder.bind(model);
                    }

                    @Override
                    protected void onLoadingStateChanged(@NonNull LoadingState state) {
                        switch (state) {
                            case LOADING_INITIAL:
                            case LOADING_MORE:
                                mProgressBar.setVisibility(View.VISIBLE);
                                break;
                            case LOADED:
                                mProgressBar.setVisibility(View.GONE);
                                break;
                            case FINISHED:
                                mProgressBar.setVisibility(View.GONE);
                                showToast("Reached end of data set.");
                                break;
                            case ERROR:
                                showToast("An error occurred.");
                                retry();
                                break;
                        }
                    }
                };

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_firestore_paging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_add_data) {
            showToast("Adding data...");
            createItems().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        showToast("Data added.");
                    } else {
                        Log.w(TAG, "addData", task.getException());
                        showToast("Error adding data.");
                    }
                }
            });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private Task<Void> createItems() {
        WriteBatch writeBatch = mFirestore.batch();

        for (int i = 0; i < 250; i++) {
            String title = "Item " + i;

            String id = String.format(Locale.getDefault(), "item_%03d", i);
            Item item = new Item(title, i);

            writeBatch.set(mItemsCollection.document(id), item);
        }

        return writeBatch.commit();
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public static class Item {

        @Nullable public String text;
        public int value;

        public Item() {}

        public Item(@Nullable String text, int value) {
            this.text = text;
            this.value = value;
        }

    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_text)
        TextView mTextView;

        @BindView(R.id.item_value)
        TextView mValueView;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(@NonNull Item item) {
            mTextView.setText(item.text);
            mValueView.setText(String.valueOf(item.value));
        }
    }

}
