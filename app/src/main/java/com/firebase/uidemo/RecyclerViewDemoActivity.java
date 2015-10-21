package com.firebase.uidemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.ui.FirebaseLoginBaseActivity;
import com.firebase.ui.FirebaseRecyclerViewAdapter;
import com.firebase.ui.com.firebasei.ui.authimpl.SocialProvider;


public class RecyclerViewDemoActivity extends FirebaseLoginBaseActivity {

    public static String TAG = "FirebaseUI.chat";
    private Firebase mRef;
    private AuthData mAuthData;
    private Button mSendButton;
    private EditText mMessageEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        final String name = "Android User";
        mSendButton = (Button) findViewById(R.id.sendButton);
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);
        final RecyclerView messages = (RecyclerView) findViewById(R.id.messagesList);
        messages.setHasFixedSize(true);
        messages.setLayoutManager(new LinearLayoutManager(this));

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = new Chat(name, mMessageEdit.getText().toString());
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

        Query recentMessages = mRef.limitToLast(50);
        FirebaseRecyclerViewAdapter<Chat, ChatHolder> adapter = new FirebaseRecyclerViewAdapter<Chat, ChatHolder>(Chat.class, android.R.layout.two_line_list_item, ChatHolder.class, recentMessages) {
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

    public static final int LOGIN = Menu.FIRST;
    public static final int LOGOUT = LOGIN+1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(LOGIN, LOGIN, LOGIN, "Log in");
        menu.add(LOGOUT, LOGOUT, LOGOUT, "Log out");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(LOGIN-Menu.FIRST).setVisible(mAuthData == null);
        menu.getItem(LOGOUT-Menu.FIRST).setVisible(mAuthData != null);
        mSendButton.setEnabled(mAuthData != null);
        mMessageEdit.setEnabled(mAuthData != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case LOGIN:
                this.loginWithProvider(SocialProvider.google);
                return true;
            case LOGOUT:
                this.logout();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    // Start of FirebaseLoginBaseActivity

    @Override
    public void onFirebaseLogin(AuthData authData) {
        Log.i(TAG, "Logged in");
        mAuthData = authData;
        invalidateOptionsMenu();
    }

    @Override
    public void onFirebaseLogout() {
        Log.i(TAG, "Logged out");
        mAuthData = null;
        invalidateOptionsMenu();
    }

    @Override
    public void onFirebaseLoginError(FirebaseError firebaseError) {
        Log.e(TAG, firebaseError.toString());
    }

    @Override
    public void onFirebaseLoginCancel() {
        Log.i(TAG, "Login cancelled");
    }

    @Override
    public Firebase setupFirebase() {
        if (mRef == null) {
            mRef = new Firebase("https://firebaseui.firebaseio.com/chat");
        }

        return mRef;
    }

    // End of FirebaseLoginBaseActivity


    public static class Chat {
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

    public static class ChatHolder extends RecyclerView.ViewHolder {
        TextView nameView, textView;

        public ChatHolder(View itemView) {
            super(itemView);
            nameView = (TextView) itemView.findViewById(android.R.id.text2);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
