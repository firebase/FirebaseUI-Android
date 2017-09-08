package com.firebase.uidemo.database.realtime;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.uidemo.R;
import com.firebase.uidemo.database.Chat;
import com.firebase.uidemo.database.ChatHolder;
import com.firebase.uidemo.database.DatabaseActivityBase;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

/**
 * Class demonstrating how to add data to a ref and then read it back using the {@link
 * FirebaseRecyclerAdapter} to build a simple chat app.
 * <p>
 * For an example on how to initialize the {@link RecyclerView}, see the {@link
 * DatabaseActivityBase}.
 */
public class RealtimeDbChatActivity extends DatabaseActivityBase {
    private static final String TAG = "RealtimeDatabaseDemo";

    /**
     * Get the last 50 chat messages.
     */
    protected static final Query sChatQuery =
            FirebaseDatabase.getInstance().getReference().child("chats").limitToLast(50);

    @Override
    protected RecyclerView.Adapter newAdapter() {
        return new FirebaseRecyclerAdapter<Chat, ChatHolder>(
                Chat.class,
                R.layout.message,
                ChatHolder.class,
                sChatQuery,
                this) {
            @Override
            public void populateViewHolder(ChatHolder holder, Chat chat, int position) {
                holder.bind(chat);
            }
        };
    }

    @Override
    protected void onAddMessage(Chat chat) {
        sChatQuery.getRef().push().setValue(chat, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference reference) {
                if (error != null) {
                    Log.e(TAG, "Failed to write message", error.toException());
                }
            }
        });
    }
}
