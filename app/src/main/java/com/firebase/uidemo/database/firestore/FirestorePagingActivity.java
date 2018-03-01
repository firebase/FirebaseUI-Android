package com.firebase.uidemo.database.firestore;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreInfiniteScrollListener;
import com.firebase.ui.firestore.FirestorePagingAdapter;
import com.firebase.ui.firestore.FirestorePagingOptions;
import com.firebase.uidemo.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FirestorePagingActivity extends AppCompatActivity {

    @BindView(R.id.paging_recycler)
    RecyclerView mRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firestore_paging);
        ButterKnife.bind(this);

        setUpAdapter();
    }

    private void setUpAdapter() {
        Query baseQuery = FirebaseFirestore.getInstance()
                .collection("items")
                .orderBy("value", Query.Direction.ASCENDING);

        FirestorePagingOptions<Item> options = new FirestorePagingOptions.Builder<Item>()
                .setQuery(baseQuery, Item.class)
                .setPageSize(10)
                .setLoadTriggerDistance(5)
                .setMaxPages(3)
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
                };

        LinearLayoutManager manager = new LinearLayoutManager(this);

        FirestoreInfiniteScrollListener listener =
                new FirestoreInfiniteScrollListener(manager, adapter);

        mRecycler.setLayoutManager(manager);
        mRecycler.setAdapter(adapter);
        mRecycler.addOnScrollListener(listener);
    }

    // TODO: Bind this to a menu item
    public Task<Void> createItems() {

        WriteBatch writeBatch = FirebaseFirestore.getInstance().batch();
        CollectionReference collRef = FirebaseFirestore.getInstance().collection("items");

        for (int i = 0; i < 500; i++) {
            String title = "Item " + i;

            String id = String.format(Locale.getDefault(), "item_%03d", i);
            Item item = new Item(title, i);

            writeBatch.set(collRef.document(id), item);
        }

        return writeBatch.commit();
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
