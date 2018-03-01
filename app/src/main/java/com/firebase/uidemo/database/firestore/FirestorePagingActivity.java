package com.firebase.uidemo.database.firestore;

import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.firebase.ui.firestore.FirestoreInfiniteScrollListener;
import com.firebase.ui.firestore.FirestorePagingAdapter;
import com.firebase.ui.firestore.FirestorePagingOptions;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firestore_paging);
        ButterKnife.bind(this);

        mFirestore = FirebaseFirestore.getInstance();
        mProgressBar.setIndeterminate(true);

        setUpAdapter();
    }

    private void setUpAdapter() {
        Query baseQuery = mFirestore.collection("items")
                .orderBy("value", Query.Direction.ASCENDING);

        // Paging options:
        //  * pageSize - number of items to load in each 'page'.
        //  * loadTriggerDistance - how far from the bottom/top of the data set to trigger a load.
        //  * maxPages - maximum number of pages to keep in memory at a time.
        FirestorePagingOptions<Item> options = new FirestorePagingOptions.Builder<Item>()
                .setQuery(baseQuery, Item.class)
                .setPageSize(20)
                .setLoadTriggerDistance(10)
                .setMaxPages(5)
                .build();

        FirestorePagingAdapter<Item, ItemViewHolder> adapter =
                new FirestorePagingAdapter<Item, ItemViewHolder>(options) {
                    @Override
                    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_item, parent, false);

                        return new ItemViewHolder(view);
                    }

                    @Override
                    protected void onBindViewHolder(ItemViewHolder holder, int position, Item model) {
                        holder.bind(model);
                    }

                    @Override
                    protected void onLoadingStateChanged(boolean isLoading) {
                        super.onLoadingStateChanged(isLoading);
                        if (isLoading) {
                            mProgressBar.setVisibility(View.VISIBLE);
                        } else {
                            mProgressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                };

        LinearLayoutManager manager = new LinearLayoutManager(this);

        // The infinite scroll listener will instruct the adapter to page up and down at the
        // appropriate times.
        FirestoreInfiniteScrollListener listener =
                new FirestoreInfiniteScrollListener(manager, adapter);

        mRecycler.setLayoutManager(manager);
        mRecycler.setAdapter(adapter);
        mRecycler.addOnScrollListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_firestore_paging, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    private Task<Void> createItems() {

        WriteBatch writeBatch = mFirestore.batch();
        CollectionReference collRef = mFirestore.collection("items");

        for (int i = 0; i < 500; i++) {
            String title = "Item " + i;

            String id = String.format(Locale.getDefault(), "item_%03d", i);
            Item item = new Item(title, i);

            writeBatch.set(collRef.document(id), item);
        }

        return writeBatch.commit();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public static class Item {

        public String text;
        public int value;

        public Item() {}

        public Item(String text, int value) {
            this.text = text;
            this.value = value;
        }

    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_text)
        TextView mTextView;

        @BindView(R.id.item_value)
        TextView mValueView;

        ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(Item item) {
            mTextView.setText(item.text);
            mValueView.setText(String.valueOf(item.value));
        }
    }

}
