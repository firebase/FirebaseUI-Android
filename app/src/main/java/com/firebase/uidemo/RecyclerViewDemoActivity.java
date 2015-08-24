package com.firebase.uidemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.firebase.ui.FirebaseRecyclerViewAdapter;


public class RecyclerViewDemoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        final Firebase ref = new Firebase("https://nanochat.firebaseio.com");

        final String name = "Android User";
        final Button sendButton = (Button) findViewById(R.id.sendButton);
        final EditText messageEdit = (EditText) findViewById(R.id.messageEdit);
        final RecyclerView messages = (RecyclerView) findViewById(R.id.messagesList);
        messages.setHasFixedSize(true);
        messages.setLayoutManager(new LinearLayoutManager(this));

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = new Chat(name, messageEdit.getText().toString());
                ref.push().setValue(chat);
                messageEdit.setText("");
            }
        });

        FirebaseRecyclerViewAdapter<Chat, ChatHolder> adapter =
                new FirebaseRecyclerViewAdapter<Chat, ChatHolder>(
                        Chat.class, android.R.layout.two_line_list_item, ChatHolder.class, ref) {
            @Override
            public void populateViewHolder(ChatHolder chatView, Chat chat) {
                chatView.messageText.setText(chat.getMessage());
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

    }


    public static class Chat {
        String name;
        String message;

        public Chat() {
        }

        public Chat(String name, String message) {
            this.name = name;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
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
