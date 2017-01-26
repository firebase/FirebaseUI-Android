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

import android.content.Context;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

@SuppressWarnings("LogConditional")
public class ChatActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {
    private static final String TAG = "RecyclerViewDemo";

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private DatabaseReference mChatRef;
    private Button mSendButton;
    private EditText mMessageEdit;

    private RecyclerView mMessages;
    private LinearLayoutManager mManager;
    private FirebaseRecyclerAdapter<Chat, ChatHolder> mRecyclerViewAdapter;
    private View mEmptyListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(this);

        mSendButton = (Button) findViewById(R.id.sendButton);
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);

        mEmptyListView = findViewById(R.id.emptyTextView);

        mRef = FirebaseDatabase.getInstance().getReference();
        mChatRef = mRef.child("chats");

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = mAuth.getCurrentUser().getUid();
                String name = "User " + uid.substring(0, 6);

                Chat chat = new Chat(name, mMessageEdit.getText().toString(), uid);
                mChatRef.push().setValue(chat, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference reference) {
                        if (error != null) {
                            Log.e(TAG, "Failed to write message", error.toException());
                        }
                    }
                });

                mMessageEdit.setText("");
            }
        });

        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(false);

        mMessages = (RecyclerView) findViewById(R.id.messagesList);
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
                chatView.setText(chat.getMessage());

                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && chat.getUid().equals(currentUser.getUid())) {
                    chatView.setIsSender(true);
                } else {
                    chatView.setIsSender(false);
                }
            }

            @Override
            protected void onDataChanged() {
                // if there are no chat messages, show a view that invites the user to add a message
                mEmptyListView.setVisibility(mRecyclerViewAdapter.getItemCount() == 0 ?
                        View.VISIBLE : View.INVISIBLE);
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
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        attachRecyclerViewAdapter();
                    }
                })
                .addOnCompleteListener(new SignInResultNotifier(this));
    }

    public boolean isSignedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public void updateUI() {
        // Sending only allowed when signed in
        mSendButton.setEnabled(isSignedIn());
        mMessageEdit.setEnabled(isSignedIn());
    }

    public static class Chat {
        private String mName;
        private String mMessage;
        private String mUid;

        public Chat() {
            // Needed for Firebase
        }

        public Chat(String name, String message, String uid) {
            mName = name;
            mMessage = message;
            mUid = uid;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getMessage() {
            return mMessage;
        }

        public void setMessage(String message) {
            mMessage = message;
        }

        public String getUid() {
            return mUid;
        }

        public void setUid(String uid) {
            mUid = uid;
        }
    }

    public static class ChatHolder extends RecyclerView.ViewHolder {
        private final TextView mNameField;
        private final TextView mTextField;
        private final FrameLayout mLeftArrow;
        private final FrameLayout mRightArrow;
        private final RelativeLayout mMessageContainer;
        private final LinearLayout mMessage;
        private final int mGreen300;
        private final int mGray300;

        public ChatHolder(View itemView) {
            super(itemView);
            mNameField = (TextView) itemView.findViewById(R.id.name_text);
            mTextField = (TextView) itemView.findViewById(R.id.message_text);
            mLeftArrow = (FrameLayout) itemView.findViewById(R.id.left_arrow);
            mRightArrow = (FrameLayout) itemView.findViewById(R.id.right_arrow);
            mMessageContainer = (RelativeLayout) itemView.findViewById(R.id.message_container);
            mMessage = (LinearLayout) itemView.findViewById(R.id.message);
            mGreen300 = ContextCompat.getColor(itemView.getContext(), R.color.material_green_300);
            mGray300 = ContextCompat.getColor(itemView.getContext(), R.color.material_gray_300);
        }

        public void setIsSender(boolean isSender) {
            final int color;
            if (isSender) {
                color = mGreen300;
                mLeftArrow.setVisibility(View.GONE);
                mRightArrow.setVisibility(View.VISIBLE);
                mMessageContainer.setGravity(Gravity.END);
            } else {
                color = mGray300;
                mLeftArrow.setVisibility(View.VISIBLE);
                mRightArrow.setVisibility(View.GONE);
                mMessageContainer.setGravity(Gravity.START);
            }

            ((GradientDrawable) mMessage.getBackground()).setColor(color);
            ((RotateDrawable) mLeftArrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
            ((RotateDrawable) mRightArrow.getBackground()).getDrawable()
                    .setColorFilter(color, PorterDuff.Mode.SRC);
        }

        public void setName(String name) {
            mNameField.setText(name);
        }

        public void setText(String text) {
            mTextField.setText(text);
        }
    }

    /**
     * Notifies the user of sign in successes or failures beyond the lifecycle of an activity.
     */
    private static class SignInResultNotifier implements OnCompleteListener<AuthResult> {
        private Context mContext;

        public SignInResultNotifier(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                Toast.makeText(mContext, R.string.signed_in, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, R.string.sign_in_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
