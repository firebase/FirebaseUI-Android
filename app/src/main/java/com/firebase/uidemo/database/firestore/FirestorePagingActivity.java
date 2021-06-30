package com.firebase.uidemo.database.firestore;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.uidemo.R;
import com.firebase.uidemo.databinding.ActivityFirestorePagingBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.paging.PagingConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class FirestorePagingActivity extends AppCompatActivity {

    private static final String TAG = "FirestorePagingActivity";

    private ActivityFirestorePagingBinding mBinding;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItemsCollection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityFirestorePagingBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mFirestore = FirebaseFirestore.getInstance();
        mItemsCollection = mFirestore.collection("items");

        setUpAdapter();
    }

    private void setUpAdapter() {
        Query baseQuery = mItemsCollection.orderBy("value", Query.Direction.ASCENDING);

        PagingConfig config = new PagingConfig(20, 10, false);

        FirestorePagingOptions<Item> options = new FirestorePagingOptions.Builder<Item>()
                .setLifecycleOwner(this)
                .setQuery(baseQuery, config, Item.class)
                .build();

        final FirestorePagingAdapter<Item, ItemViewHolder> adapter =
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
                };
        adapter.addLoadStateListener(states -> {
            LoadState refresh = states.getRefresh();
            LoadState append = states.getAppend();

            if (refresh instanceof LoadState.Error || append instanceof LoadState.Error) {
                showToast("An error occurred.");
                adapter.retry();
            }

            if (append instanceof LoadState.Loading) {
                mBinding.swipeRefreshLayout.setRefreshing(true);
            }

            if (append instanceof LoadState.NotLoading) {
                LoadState.NotLoading notLoading = (LoadState.NotLoading) append;
                if (notLoading.getEndOfPaginationReached()) {
                    // This indicates that the user has scrolled
                    // until the end of the data set.
                    mBinding.swipeRefreshLayout.setRefreshing(false);
                    showToast("Reached end of data set.");
                    return null;
                }

                if (refresh instanceof LoadState.NotLoading) {
                    // This indicates the most recent load
                    // has finished.
                    mBinding.swipeRefreshLayout.setRefreshing(false);
                    return null;
                }
            }
            return null;
        });

        mBinding.pagingRecycler.setLayoutManager(new LinearLayoutManager(this));
        mBinding.pagingRecycler.setAdapter(adapter);

        mBinding.swipeRefreshLayout.setOnRefreshListener(() -> adapter.refresh());
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_paging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_add_data) {
            showToast("Adding data...");
            createItems().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    showToast("Data added.");
                } else {
                    Log.w(TAG, "addData", task.getException());
                    showToast("Error adding data.");
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
        TextView mTextView;
        TextView mValueView;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.item_text);
            mValueView = itemView.findViewById(R.id.item_value);
        }

        void bind(@NonNull Item item) {
            mTextView.setText(item.text);
            mValueView.setText(String.valueOf(item.value));
        }
    }

}
