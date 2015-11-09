package com.firebase.uidemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.ui.FirebaseRecyclerViewAdapter;


public class RecyclerViewDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        final Firebase ref = new Firebase("https://firebaseui.firebaseio.com/chat");

        final String name = "PR User";
        final Button sendButton = (Button) findViewById(R.id.sendButton);
        final EditText messageEdit = (EditText) findViewById(R.id.messageEdit);
        final RecyclerView messages = (RecyclerView) findViewById(R.id.messagesList);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = new Chat(name, messageEdit.getText().toString());
                ref.push().setValue(chat, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if (firebaseError != null) {
                            Log.e("FirebaseUI.chat", firebaseError.toString());
                        }
                    }
                });
                messageEdit.setText("");
            }
        });
        final FirebaseRecyclerViewAdapter<Chat, RecyclerView.ViewHolder> adapter = new Adapter(
                Chat.class,
                android.R.layout.two_line_list_item,
                ref,
                15,
                false,
                name );

        layoutManager.setReverseLayout(true);
        messages.setHasFixedSize(true);
        messages.setLayoutManager(layoutManager);
        messages.setAdapter(adapter);

        messages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    return;
                }
                if (layoutManager.findLastVisibleItemPosition() < adapter.getItemCount() - 20) {
                    return;
                }
                adapter.more();
            }
        });
    }


    public static class Chat {
        String name;
        String text;

        public Chat() {
        }

        public Chat(String name, String message) {
            this.name = name;
            this.text = message;
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }
    }

    public static class HeaderHolder extends  RecyclerView.ViewHolder {
        public HeaderHolder(View itemView) {
            super(itemView);
        }
    }

    public static class FooterHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public FooterHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
        }
    }


    public static class ChatHolder extends RecyclerView.ViewHolder {
        TextView nameView, textView;

        public ChatHolder(View itemView) {
            super(itemView);
            nameView = (TextView) itemView.findViewById(android.R.id.text2);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    public static class Adapter extends FirebaseRecyclerViewAdapter<Chat, RecyclerView.ViewHolder> {

        public static final String TAG = Adapter.class.getSimpleName();

        public static int VIEW_TYPE_FOOTER = 0;
        public static int VIEW_TYPE_CONTENT = 1;
        public static int VIEW_TYPE_HEADER = 2;
        private String name;
        private boolean synced;

        public Adapter(Class<Chat> modelClass, int modelLayout, Query ref, int pageSize, boolean orderASC, String name) {
            super(modelClass, modelLayout, RecyclerView.ViewHolder.class, ref, pageSize, orderASC);
            this.name = name;
        }

        @Override
        public int getItemViewType(int position) {
            if (position  == getItemCount() - 1) {
                return VIEW_TYPE_FOOTER;
            }
            else if(position == 0) {
                return VIEW_TYPE_HEADER;
            }
            return VIEW_TYPE_CONTENT;
        }

        @Override
        public int getItemCount() {
            return super.getItemCount() + 2;
        }

        @Override
        public int getSnapShotOffset() {
            return 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if(viewType == VIEW_TYPE_FOOTER) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_progress, parent, false);
                return new FooterHolder(view);
            }
            else if(viewType == VIEW_TYPE_HEADER) {
                view = new LinearLayout(parent.getContext());
                view.setMinimumHeight(1);
                return new HeaderHolder(view);
            }
            else {
                view = LayoutInflater.from(parent.getContext()).inflate(mModelLayout, parent, false);
                return new ChatHolder(view);
            }

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            int itemViewType = getItemViewType(position);
            if(itemViewType == VIEW_TYPE_FOOTER) {
                FooterHolder footerHolder = (FooterHolder) viewHolder;
                footerHolder.progressBar.setVisibility(synced ? View.GONE : View.VISIBLE);
            }
            else if(itemViewType == VIEW_TYPE_CONTENT) {
                super.onBindViewHolder(viewHolder, position);
            }
        }

        @Override
        public void populateViewHolder(RecyclerView.ViewHolder viewHolder, Chat chat) {
            ChatHolder chatView = (ChatHolder) viewHolder;
            chatView.textView.setText(chat.getText());
            chatView.textView.setPadding(10, 0, 10, 0);
            chatView.nameView.setText(chat.getName());
            chatView.nameView.setPadding(10, 0, 10, 15);
            if (chat.getName().equals(name)) {
                chatView.textView.setGravity(Gravity.END);
                chatView.nameView.setGravity(Gravity.END);
                chatView.nameView.setTextColor(Color.parseColor("#8BC34A"));
            } else {
                chatView.textView.setGravity(Gravity.START);
                chatView.nameView.setGravity(Gravity.START);
                chatView.nameView.setTextColor(Color.parseColor("#00BCD4"));
            }
        }

        @Override
        protected void onSyncStatusChanged(boolean synced) {
            this.synced = synced;
            notifyItemChanged(getItemCount() - 1);
        }

        @Override
        protected void onArrayError(FirebaseError firebaseError) {
            Log.d(TAG, firebaseError.toString(), firebaseError.toException());
        }

    }
}
