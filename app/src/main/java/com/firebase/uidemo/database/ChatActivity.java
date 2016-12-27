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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
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

        // Allow hitting our custom Send button to send the message
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCurrentMessage();
            }
        });
        // Allow hitting the Send keyon the soft keyboard to send the message
        mMessageEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) sendCurrentMessage();
                return true;
            }
        });

        mMessages = (RecyclerView) findViewById(R.id.messagesList);

        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(false);

        mMessages.setHasFixedSize(false);
        mMessages.setLayoutManager(mManager);
    }

    private void sendCurrentMessage() {
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

    @Override
    public void onStart() {
        super.onStart();

        // Default Database rules do not allow unauthenticated reads, so we need to
        // sign in before attaching the RecyclerView adapter otherwise the Adapter will
        // not be able to read any data from the Database.
        if (!isSignedIn()) {
            signInAnonymously();
        } else {
            attachRecyclerAdapter();
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

    private void attachRecyclerAdapter() {
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
                chatView.itemView.setTag(R.layout.message, chatView);
                chatView.itemView.setTag(position);
            }

            @Override
            protected void onReady() {
                findViewById(R.id.emptyTextView).setVisibility(mRecyclerViewAdapter.getItemCount() == 0?View.VISIBLE:View.INVISIBLE);
            }
        };

        // Scroll to bottom on new messages
        mRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mManager.smoothScrollToPosition(mMessages, null, mRecyclerViewAdapter.getItemCount());
            }
        });

        // Listen for chat messages on the entire recycler view
        mMessages.setOnTouchListener(new ChatTouchListener(mMessages, mRecyclerViewAdapter));

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
                            attachRecyclerAdapter();
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
        private final TextView mNameField;
        private final TextView mTextField;
        private final FrameLayout mLeftArrow;
        private final FrameLayout mRightArrow;
        private final FrameLayout mMessageRow;
        private final RelativeLayout mMessageContainer;
        private final LinearLayout mMessage;
        private final TextView mRemoveText;
        private final int mGreen300;
        private final int mGray300;

        public ChatHolder(View itemView) {
            super(itemView);
            mNameField = (TextView) itemView.findViewById(R.id.name_text);
            mTextField = (TextView) itemView.findViewById(R.id.message_text);
            mLeftArrow = (FrameLayout) itemView.findViewById(R.id.left_arrow);
            mRightArrow = (FrameLayout) itemView.findViewById(R.id.right_arrow);
            mMessageRow = (FrameLayout) itemView.findViewById(R.id.message_row);
            mMessageContainer = (RelativeLayout) itemView.findViewById(R.id.message_container);
            mMessage = (LinearLayout) itemView.findViewById(R.id.message);
            mRemoveText = (TextView) itemView.findViewById(R.id.remove_text);
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
            ((RotateDrawable) mLeftArrow.getBackground()).getDrawable().setColorFilter(color, PorterDuff.Mode.SRC);
            ((RotateDrawable) mRightArrow.getBackground()).getDrawable().setColorFilter(color, PorterDuff.Mode.SRC);
        }

        public void setName(String name) {
            mNameField.setText(name);
        }

        public void setText(String text) {
            mTextField.setText(text);
        }

        public boolean slideMessage(int x) { return slideMessage(x, false); }
        public boolean slideMessage(int x, boolean forceDisable) {
            mMessageRow.setTranslationX(-x);
            mRemoveText.setTranslationX(mRemoveText.getWidth()-Math.min(x, mRemoveText.getWidth()));
            mRemoveText.setEnabled(x >= mRemoveText.getWidth() && !forceDisable);
            return x >= mRemoveText.getWidth();
        }
        public void resetSlide() {
            // animate the message bubble and the remove label back to their original positions
            animateRight(mMessageRow, -1*mMessageRow.getTranslationX());
            animateRight(mRemoveText, mRemoveText.getWidth() - mRemoveText.getTranslationX());
        }
        // Helper function that animates the view to the right by offsetX over a 500ms
        public static void animateRight(final View view, final float offsetX) {
            TranslateAnimation animation = new TranslateAnimation(0.0f, offsetX, 0.0f, 0.0f);
            animation.setInterpolator(new BounceInterpolator());
            animation.setDuration(500);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) { }
                @Override public void onAnimationRepeat(Animation animation) { }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.clearAnimation();
                    view.setTranslationX(view.getTranslationX()+offsetX);
                }

            });
            view.startAnimation(animation);

        }
    }
    // This class listens for touch events on the chat recycler view
    class ChatTouchListener implements View.OnTouchListener {
        private boolean mActionEnabled = false;
        private int mActionDownX = 0;
        private int mActionUpX = 0;
        private int mDifference = 0;
        RecyclerView mRecyclerView;
        FirebaseRecyclerAdapter mAdapter;
        private ChatHolder mSelectedViewHolder; // the VH the user originally touched in this move
        private int mSelectedItemPosition;

        public ChatTouchListener(RecyclerView recyclerView, FirebaseRecyclerAdapter adapter) {
            mRecyclerView = recyclerView;
            mAdapter = adapter;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            View child = mRecyclerView.findChildViewUnder(event.getX(), event.getY());
            ChatHolder overViewHolder = null;
            if (child != null) {
                overViewHolder = (ChatHolder) child.getTag(R.layout.message);
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mActionEnabled = false;
                    if (mSelectedViewHolder != null) {
                        // the user clicked on an item, while we thought they were holding another item
                        mSelectedViewHolder.slideMessage(0);
                    }
                    if (child != null) {
                        mSelectedViewHolder = overViewHolder;
                        mSelectedItemPosition = (Integer) child.getTag();
                        mActionDownX = (int) event.getX();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mSelectedViewHolder != null) {
                        mActionUpX = (int) event.getX();
                        mDifference = mActionDownX - mActionUpX;
                        mActionEnabled = mSelectedViewHolder.slideMessage(mDifference, overViewHolder != mSelectedViewHolder);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mSelectedViewHolder != null) {
                        if (mActionEnabled && mSelectedViewHolder == overViewHolder) {
                            mAdapter.getRef(mSelectedItemPosition).removeValue();
                        }
                        mActionDownX = mActionUpX = mDifference = 0;
                        mSelectedViewHolder.resetSlide();
                        mSelectedViewHolder = null;
                        mActionEnabled = false;
                        break;
                    }

            }
            return true;
        }
    }
}
