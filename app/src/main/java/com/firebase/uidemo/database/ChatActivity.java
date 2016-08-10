/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.uidemo.database;

import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.uidemo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class ChatActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    public static final String TAG = "RecyclerViewDemo";

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private DatabaseReference mChatRef;
    private Button mSendButton;
    private EditText mMessageEdit;

    private RecyclerView mMessages;
    private LinearLayoutManager mManager;
    private FirebaseRecyclerAdapter<Chat, ChatHolder> mRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(this);

        mSendButton = (Button) findViewById(R.id.sendButton);
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);

        mRef = FirebaseDatabase.getInstance().getReference();
        mChatRef = mRef.child("chats");

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = mAuth.getCurrentUser().getUid();
                String name = "User " + uid.substring(0, 6);

                Chat chat = new Chat(name, uid, mMessageEdit.getText().toString());
                mChatRef.push().setValue(chat, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference reference) {
                        if (databaseError != null) {
                            Log.e(TAG, "Failed to write message", databaseError.toException());
                        }
                    }
                });

                mMessageEdit.setText("");
            }
        });

        mMessages = (RecyclerView) findViewById(R.id.messagesList);

        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(false);

        mMessages.setHasFixedSize(false);
        mMessages.setLayoutManager(mManager);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Default Database rules do not allow unauthenticated reads, so we need to
        // sign in before attaching the RecyclerView adapter otherwise the Adapter will
        // not be able to read any data from the Database.
        if (!isSignedIn()) {
            signInAnonymously();
        } else {
            attachRecyclerViewAdapter();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecyclerViewAdapter != null) {
            mRecyclerViewAdapter.cleanup();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAuth != null) {
            mAuth.removeAuthStateListener(this);
        }
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        updateUI();
    }

    private void attachRecyclerViewAdapter() {
        Query lastFifty = mChatRef.limitToLast(50);
        mRecyclerViewAdapter = new FirebaseRecyclerAdapter<Chat, ChatHolder>(
                Chat.class, R.layout.message, ChatHolder.class, lastFifty) {

            @Override
            public void populateViewHolder(ChatHolder chatView, Chat chat, int position) {
                chatView.setName(chat.getName());
                chatView.setText(chat.getText());

                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && chat.getUid().equals(currentUser.getUid())) {
                    chatView.setIsSender(true);
                } else {
                    chatView.setIsSender(false);
                }
            }
        };

        // Scroll to bottom on new messages
        mRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mManager.smoothScrollToPosition(mMessages, null, mRecyclerViewAdapter.getItemCount());
            }
        });

        mMessages.setAdapter(mRecyclerViewAdapter);
    }

    private void signInAnonymously() {
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                        if (task.isSuccessful()) {
                            Toast.makeText(ChatActivity.this, "Signed In",
                                    Toast.LENGTH_SHORT).show();
                            attachRecyclerViewAdapter();
                        } else {
                            Toast.makeText(ChatActivity.this, "Sign In Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public boolean isSignedIn() {
        return (mAuth.getCurrentUser() != null);
    }

    public void updateUI() {
        // Sending only allowed when signed in
        mSendButton.setEnabled(isSignedIn());
        mMessageEdit.setEnabled(isSignedIn());
    }

    public static class Chat {

        String name;
        String text;
        String uid;

        public Chat() {
        }

        public Chat(String name, String uid, String message) {
            this.name = name;
            this.text = message;
            this.uid = uid;
        }

        public String getName() {
            return name;
        }

        public String getUid() {
            return uid;
        }

        public String getText() {
            return text;
        }
    }

    public static class ChatHolder extends RecyclerView.ViewHolder {
        View mView;

        public ChatHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setIsSender(Boolean isSender) {
            FrameLayout left_arrow = (FrameLayout) mView.findViewById(R.id.left_arrow);
            FrameLayout right_arrow = (FrameLayout) mView.findViewById(R.id.right_arrow);
            RelativeLayout messageContainer = (RelativeLayout) mView.findViewById(R.id.message_container);
            LinearLayout message = (LinearLayout) mView.findViewById(R.id.message);

            int color;
            if (isSender) {
                color = ContextCompat.getColor(mView.getContext(), R.color.material_green_300);

                left_arrow.setVisibility(View.GONE);
                right_arrow.setVisibility(View.VISIBLE);
                messageContainer.setGravity(Gravity.END);
            } else {
                color = ContextCompat.getColor(mView.getContext(), R.color.material_gray_300);

                left_arrow.setVisibility(View.VISIBLE);
                right_arrow.setVisibility(View.GONE);
                messageContainer.setGravity(Gravity.START);
            }

            ((GradientDrawable) message.getBackground()).setColor(color);
            ((RotateDrawable) left_arrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
            ((RotateDrawable) right_arrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
        }

        public void setName(String name) {
            TextView field = (TextView) mView.findViewById(R.id.name_text);
            field.setText(name);
        }

        public void setText(String text) {
            TextView field = (TextView) mView.findViewById(R.id.message_text);
            field.setText(text);
        }
    }
}
