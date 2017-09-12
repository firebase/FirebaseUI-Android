package com.firebase.uidemo.database.realtime;

import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.uidemo.R;
import com.firebase.uidemo.database.ChatHolder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatIndexActivity extends ChatActivity {
    private DatabaseReference mChatIndicesRef;

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
        mChatIndicesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chatIndices")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        FirebaseRecyclerOptions<Chat, ChatHolder> options =
                new FirebaseRecyclerOptions.Builder<Chat, ChatHolder>()
                        .setIndexedQuery(mChatIndicesRef.limitToFirst(50), mChatRef, Chat.class)
                        .setViewHolder(R.layout.message, ChatHolder.class)
                        .setLifecycleOwner(this)
                        .build();

        return new FirebaseRecyclerAdapter<Chat, ChatHolder>(options) {
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
