package com.firebase.uidemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.firebase.ui.FirebaseRecyclerViewAdapter;


public class RecyclerViewDemoActivity extends AppCompatActivity {

    private static final int INITIAL_LOAD_COUNT = 15;
    private static final int INCREMENTAL_LOAD_COUNT = 15;
    private static final int PREFETCH_THRESHOLD = 5;

    private int mLastLimitCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        final Firebase ref = new Firebase("https://firebaseui.firebaseio.com/largechat");

        final String name = "Android User";
        final Button sendButton = (Button) findViewById(R.id.sendButton);
        final EditText messageEdit = (EditText) findViewById(R.id.messageEdit);
        final RecyclerView messages = (RecyclerView) findViewById(R.id.messagesList);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        messages.setHasFixedSize(true);
        messages.setLayoutManager(layoutManager);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = new Chat(name, messageEdit.getText().toString());
                ref.push().setValue(chat);
                messageEdit.setText("");
            }
        });

        mLastLimitCount = INITIAL_LOAD_COUNT;
        final FirebaseRecyclerViewAdapter<Chat, ChatHolder> adapter =
                new FirebaseRecyclerViewAdapter<Chat, ChatHolder>(Chat.class,
                        android.R.layout.two_line_list_item, ChatHolder.class,
                        ref.orderByKey().limitToLast(mLastLimitCount)) {
                    @Override
                    public void populateViewHolder(ChatHolder chatView, Chat chat) {
                        chatView.messageText.setText(chat.getText());
                        chatView.messageText.setPadding(10, 0, 10, 0);
                        chatView.nameText.setText(chat.getName());
                        chatView.nameText.setPadding(10, 0, 10, 15);
                        if (chat.getName().equals(name)) {
                            chatView.messageText.setGravity(Gravity.END);
                            chatView.nameText.setGravity(Gravity.END);
                            chatView.nameText.setTextColor(Color.parseColor("#8BC34A"));
                        } else {
                            chatView.nameText.setTextColor(Color.parseColor("#00BCD4"));
                        }
                    }
                };

        messages.setAdapter(adapter);

        messages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                boolean isScrollingUp = dy < 0;
                boolean shouldPrefetch =
                        layoutManager.findFirstVisibleItemPosition() < PREFETCH_THRESHOLD;

                if (isScrollingUp && shouldPrefetch) {
                    if (shouldPrefetch) {
                        mLastLimitCount += INCREMENTAL_LOAD_COUNT;
                        adapter.updateQuery(ref.orderByKey().limitToLast(mLastLimitCount));
                    }
                }
            }
        });

    }

    public static class Chat {
        String name;
        String text;

        public Chat() {
        }

        public Chat(String name, String text) {
            this.name = name;
            this.text = text;
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }
    }

    public static class ChatHolder extends RecyclerView.ViewHolder {
        TextView nameText, messageText;

        public ChatHolder(View itemView) {
            super(itemView);
            nameText = (TextView) itemView.findViewById(android.R.id.text2);
            messageText = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
