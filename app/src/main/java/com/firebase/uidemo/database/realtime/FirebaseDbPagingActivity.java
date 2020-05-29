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
import com.firebase.ui.database.paging.LoadingState;
import com.firebase.uidemo.R;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FirebaseDbPagingActivity extends AppCompatActivity {

    private static final String TAG = "PagingActivity";

    @BindView(R.id.paging_recycler)
    RecyclerView mRecycler;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private FirebaseDatabase mDatabase;
    private Query mQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_paging);
        ButterKnife.bind(this);

        mDatabase = FirebaseDatabase.getInstance();
        mQuery = mDatabase.getReference().child("items");

        setUpAdapter();
    }

    private void setUpAdapter() {

        //Initialize Paging Configurations
        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(5)
                .setPageSize(30)
                .build();

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

                    @Override
                    protected void onLoadingStateChanged(@NonNull LoadingState state) {
                        switch (state) {
                            case LOADING_INITIAL:
                            case LOADING_MORE:
                                mSwipeRefreshLayout.setRefreshing(true);
                                break;
                            case LOADED:
                                mSwipeRefreshLayout.setRefreshing(false);
                                break;
                            case FINISHED:
                                mSwipeRefreshLayout.setRefreshing(false);
                                showToast(getString(R.string.paging_finished_message));
                                break;
                            case ERROR:
                                showToast(getString(R.string.unknown_error));
                                break;
                        }
                    }

                    @Override
                    protected void onError(DatabaseError databaseError) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, databaseError.getDetails(), databaseError.toException());
                    }
                };

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

        // Reload data on swipe
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Reload Data
                mAdapter.refresh();
            }
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
