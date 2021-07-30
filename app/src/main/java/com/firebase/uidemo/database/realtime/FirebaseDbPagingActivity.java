package com.firebase.uidemo.database.realtime;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.paging.DatabasePagingOptions;
import com.firebase.ui.database.paging.FirebaseRecyclerPagingAdapter;
import com.firebase.uidemo.R;
import com.firebase.uidemo.databinding.ActivityDatabasePagingBinding;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.paging.LoadState;
import androidx.paging.PagingConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class FirebaseDbPagingActivity extends AppCompatActivity {

    private static final String TAG = "PagingActivity";

    private ActivityDatabasePagingBinding mBinding;

    private FirebaseDatabase mDatabase;
    private Query mQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityDatabasePagingBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mDatabase = FirebaseDatabase.getInstance();
        mQuery = mDatabase.getReference().child("items");

        setUpAdapter();
    }

    private void setUpAdapter() {

        //Initialize Paging Configurations
        PagingConfig config = new PagingConfig(30, 5, false);

        //Initialize Firebase Paging Options
        DatabasePagingOptions<Item> options = new DatabasePagingOptions.Builder<Item>()
                .setLifecycleOwner(this)
                .setQuery(mQuery, config, Item.class)
                .build();

        //Initializing Adapter
        final FirebaseRecyclerPagingAdapter<Item, ItemViewHolder> mAdapter =
                new FirebaseRecyclerPagingAdapter<Item, ItemViewHolder>(options) {
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

        mAdapter.addLoadStateListener(states -> {
            LoadState refresh = states.getRefresh();
            LoadState append = states.getAppend();

            if (refresh instanceof LoadState.Error) {
                LoadState.Error loadStateError = (LoadState.Error) refresh;
                mBinding.swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, loadStateError.getError().getLocalizedMessage());
            }
            if (append instanceof LoadState.Error) {
                LoadState.Error loadStateError = (LoadState.Error) append;
                mBinding.swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, loadStateError.getError().getLocalizedMessage());
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
        mBinding.pagingRecycler.setAdapter(mAdapter);

        // Reload data on swipe
        mBinding.swipeRefreshLayout.setOnRefreshListener(() -> {
            //Reload Data
            mAdapter.refresh();
        });
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
            createItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private void createItems() {
        for (int i = 0; i < 250; i++) {
            String title = "Item " + i;

            String id = String.format(Locale.getDefault(), "item_%03d", i);
            Item item = new Item(title, i);

            mDatabase.getReference("items").child(id).setValue(item);
        }
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    public static class Item {

        @Nullable
        public String text;
        public int value;

        public Item(){}

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
