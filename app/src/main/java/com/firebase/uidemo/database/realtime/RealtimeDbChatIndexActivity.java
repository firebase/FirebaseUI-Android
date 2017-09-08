package com.firebase.uidemo.database.realtime;

import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.uidemo.R;
import com.firebase.uidemo.database.Chat;
import com.firebase.uidemo.database.ChatHolder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RealtimeDbChatIndexActivity extends RealtimeDbChatActivity {
    private DatabaseReference mChatIndicesRef;

    @Override
    protected FirebaseRecyclerAdapter<Chat, ChatHolder> newAdapter() {
        mChatIndicesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("chatIndices")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        return new FirebaseIndexRecyclerAdapter<Chat, ChatHolder>(
                Chat.class,
                R.layout.message,
                ChatHolder.class,
                mChatIndicesRef.limitToLast(50),
                sChatQuery.getRef(),
                this) {
            @Override
            public void populateViewHolder(ChatHolder holder, Chat chat, int position) {
                holder.bind(chat);
            }
        };
    }

    @Override
    protected void onAddMessage(Chat chat) {
        DatabaseReference chatRef = sChatQuery.getRef().push();
        mChatIndicesRef.child(chatRef.getKey()).setValue(true);
        chatRef.setValue(chat);
    }
}
