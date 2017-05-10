package com.firebase.uidemo.database;

import android.os.Bundle;
import android.view.View;

import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.uidemo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatIndexActivity extends ChatActivity {
    private DatabaseReference mChatIndicesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChatIndicesRef = FirebaseDatabase.getInstance().getReference().child("chatIndices");
    }

    @Override
    public void onClick(View v) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String name = "User " + uid.substring(0, 6);
        Chat chat = new Chat(name, mMessageEdit.getText().toString(), uid);

        DatabaseReference chatRef = mChatRef.push();
        mChatIndicesRef.child(chatRef.getKey()).setValue(true);
        chatRef.setValue(chat);

        mMessageEdit.setText("");
    }

    @Override
    protected FirebaseRecyclerAdapter<Chat, ChatHolder> getAdapter() {
        return new FirebaseIndexRecyclerAdapter<Chat, ChatHolder>(
                Chat.class,
                R.layout.message,
                ChatHolder.class,
                mChatIndicesRef.limitToLast(50),
                mChatRef) {
            @Override
            public void populateViewHolder(ChatHolder holder, Chat chat, int position) {
                holder.bind(chat);
            }

            @Override
            public void onDataChanged() {
                // If there are no chat messages, show a view that invites the user to add a message.
                mEmptyListMessage.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };
    }
}
