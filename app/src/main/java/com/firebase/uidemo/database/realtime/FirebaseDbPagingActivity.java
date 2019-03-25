package com.firebase.uidemo.database.realtime;

import android.arch.paging.PagedList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
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

import butterknife.BindView;
import butterknife.ButterKnife;

public class FirebaseDbPagingActivity extends AppCompatActivity {

    private final String TAG = "PagingActivity";

    @BindView(R.id.paging_recycler)
    RecyclerView mRecycler;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private Query mQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_paging);
        ButterKnife.bind(this);

        mQuery = FirebaseDatabase.getInstance().getReference().child("posts");

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
        DatabasePagingOptions<Post> options = new DatabasePagingOptions.Builder<Post>()
                .setLifecycleOwner(this)
                .setQuery(mQuery, config, Post.class)
                .build();

        //Initializing Adapter
        final FirebaseRecyclerPagingAdapter<Post, PostViewHolder> mAdapter =
                new FirebaseRecyclerPagingAdapter<Post, PostViewHolder>(options) {
                    @NonNull
                    @Override
                    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                             int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_post, parent, false);
                        return new PostViewHolder(view);
                    }

                    @Override
                    protected void onBindViewHolder(@NonNull PostViewHolder holder,
                                                    int position,
                                                    @NonNull Post model) {
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
                                Toast.makeText(getApplicationContext(), getString(R.string.paging_finished_message), Toast.LENGTH_SHORT).show();
                                break;

                            case ERROR:
                                retry();
                                break;
                        }
                    }

                    @Override
                    protected void onError(DatabaseError databaseError) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, databaseError.getMessage());
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

    public static class Post {

        @Nullable public String title;
        @Nullable public String body;

        public Post(){}

        public Post(@Nullable String title, @Nullable String body) {
            this.title = title;
            this.body = body;
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.textViewTitle)
        TextView mTitleView;

        @BindView(R.id.textViewBody)
        TextView mBodyView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(@NonNull Post post) {
            mTitleView.setText(post.title);
            mBodyView.setText(post.body);
        }
    }

}
