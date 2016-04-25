package com.firebase.uidemo;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.ui.auth.core.FirebaseLoginBaseActivity;
import com.firebase.ui.FirebaseRecyclerAdapter;
import com.firebase.ui.auth.core.FirebaseLoginError;
import com.firebase.ui.auth.core.AuthProviderType;
import com.firebase.ui.auth.core.FirebaseSignupError;

public class RecyclerViewDemoActivity extends FirebaseLoginBaseActivity {

    public static String TAG = "FirebaseUI.chat";
    private Firebase mRef;
    private Query mChatRef;
    private String mName;
    private Button mSendButton;
    private EditText mMessageEdit;

    private RecyclerView mMessages;
    private FirebaseRecyclerAdapter<Chat, ChatHolder> mRecycleViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        mSendButton = (Button) findViewById(R.id.sendButton);
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFirebaseLoginPrompt();
            }
        });

        mRef = new Firebase("https://firebaseui.firebaseio.com/chat_3");
        mChatRef = mRef.limitToLast(50);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Chat chat = new Chat(mName, getAuth().getUid(), mMessageEdit.getText().toString());
            mRef.push().setValue(chat, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    if (firebaseError != null) {
                        Log.e(TAG, firebaseError.toString());
                    }
                }
            });
            mMessageEdit.setText("");
            }
        });

        mMessages = (RecyclerView) findViewById(R.id.messagesList);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(false);

        mMessages.setHasFixedSize(false);
        mMessages.setLayoutManager(manager);

        mRecycleViewAdapter = new FirebaseRecyclerAdapter<Chat, ChatHolder>(Chat.class, R.layout.message, ChatHolder.class, mChatRef) {
            @Override
            public void populateViewHolder(ChatHolder chatView, Chat chat, int position) {
                chatView.setName(chat.getName());
                chatView.setText(chat.getText());

                if (getAuth() != null && chat.getUid().equals(getAuth().getUid())) {
                    chatView.setIsSender(true);
                } else {
                    chatView.setIsSender(false);
                }
            }
        };

        mMessages.setAdapter(mRecycleViewAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setEnabledAuthProvider(AuthProviderType.FACEBOOK);
        setEnabledAuthProvider(AuthProviderType.TWITTER);
        setEnabledAuthProvider(AuthProviderType.GOOGLE);
        setEnabledAuthProvider(AuthProviderType.PASSWORD);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_login_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.signup_menu_item).setVisible(getAuth() == null);
        menu.findItem(R.id.login_menu_item).setVisible(getAuth() == null);
        menu.findItem(R.id.logout_menu_item).setVisible(getAuth() != null);
        mSendButton.setEnabled(getAuth() != null);
        mMessageEdit.setEnabled(getAuth() != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signup_menu_item:
                this.showFirebaseSignupPrompt(true); //passing true will automatically log them in on successful signup
                return true;
            case R.id.login_menu_item:
                this.showFirebaseLoginPrompt();
                return true;
            case R.id.logout_menu_item:
                this.logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onFirebaseSignedUp() {
        Log.i(TAG, "Signed Up");
    }

    @Override
    protected void onFirebaseSignupUserError(FirebaseSignupError firebaseError) {
        Log.e(TAG, "Signup user error: "+firebaseError.toString());
    }

    @Override
    public void onFirebaseLoggedIn(AuthData authData) {
        Log.i(TAG, "Logged in to " + authData.getProvider().toString());

        switch (authData.getProvider()) {
            case "password":
                mName = (String) authData.getProviderData().get("email");
                break;
            default:
                mName = (String) authData.getProviderData().get("displayName");
                break;
        }

        invalidateOptionsMenu();
        mRecycleViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void onFirebaseLoggedOut() {
        Log.i(TAG, "Logged out");
        mName = "";
        invalidateOptionsMenu();
        mRecycleViewAdapter.notifyDataSetChanged();
    }

    @Override
    public void onFirebaseLoginUserError(FirebaseLoginError firebaseError) {
        Log.e(TAG, "Login user error: "+firebaseError.toString());
        resetFirebaseLoginPrompt();
    }

    @Override
    public void onFirebaseProviderError(FirebaseLoginError firebaseError) {
        Log.e(TAG, "Login provider error: " + firebaseError.toString());
        resetFirebaseLoginPrompt();
    }

    @Override
    public Firebase getFirebaseRef() {
        return mRef;
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


            if (isSender) {
                left_arrow.setVisibility(View.GONE);
                right_arrow.setVisibility(View.VISIBLE);
                messageContainer.setGravity(Gravity.RIGHT);
            } else {
                left_arrow.setVisibility(View.VISIBLE);
                right_arrow.setVisibility(View.GONE);
                messageContainer.setGravity(Gravity.LEFT);
            }
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
