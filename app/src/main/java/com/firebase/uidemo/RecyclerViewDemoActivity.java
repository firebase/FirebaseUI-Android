package com.firebase.uidemo;

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
import com.firebase.client.FirebaseError;
import com.firebase.ui.FirebaseRecyclerViewAdapter;


public class RecyclerViewDemoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        final Firebase ref = new Firebase("https://firebaseui.firebaseio.com/chat");

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

        FirebaseRecyclerViewAdapter<Chat, ChatHolder> adapter = new FirebaseRecyclerViewAdapter<Chat, ChatHolder>(Chat.class, android.R.layout.two_line_list_item, ChatHolder.class, ref) {
            @Override
            public void populateViewHolder(ChatHolder chatView, Chat chat) {
                chatView.textView.setText(chat.getText());
                chatView.textView.setPadding(10, 0, 10, 0);
                chatView.nameView.setText(chat.getName());
                chatView.nameView.setPadding(10, 0, 10, 15);
                if (chat.getName().equals(name)) {
                    chatView.textView.setGravity(Gravity.END);
                    chatView.nameView.setGravity(Gravity.END);
                    chatView.nameView.setTextColor(Color.parseColor("#8BC34A"));
                } else {
                    chatView.nameView.setTextColor(Color.parseColor("#00BCD4"));
                }
            }
        };

        messages.setAdapter(adapter);
    }


    static class Chat {
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

    static class ChatHolder extends RecyclerView.ViewHolder {
        TextView nameView, textView;

        public ChatHolder(View itemView) {
            super(itemView);
            nameView = (TextView) itemView.findViewById(android.R.id.text2);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
