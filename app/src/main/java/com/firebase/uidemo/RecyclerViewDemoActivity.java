package com.firebase.uidemo;

import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.ui.FirebaseLoginBaseActivity;
import com.firebase.ui.FirebaseRecyclerViewAdapter;

public class RecyclerViewDemoActivity extends FirebaseLoginBaseActivity {

    public static String TAG = "FirebaseUI.chat";
    private Firebase mRef;
    private AuthData mAuthData;
    private String name;
    private String uid;
    private Button mSendButton;
    private EditText mMessageEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_demo);

        mSendButton = (Button) findViewById(R.id.sendButton);
        mMessageEdit = (EditText) findViewById(R.id.messageEdit);

        mRef = new Firebase("https://firebaseui.firebaseio.com/chat_3");

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = new Chat(name, mAuthData.getUid(), mMessageEdit.getText().toString());
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

        updateChat();
    }

    protected void updateChat() {
        final RecyclerView messages = (RecyclerView) findViewById(R.id.messagesList);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);

        messages.setHasFixedSize(true);
        messages.setLayoutManager(manager);

        Query recentMessages = mRef.limitToLast(50);
        FirebaseRecyclerViewAdapter<Chat, ChatHolder> adapter = new FirebaseRecyclerViewAdapter<Chat, ChatHolder>(Chat.class, R.layout.message, ChatHolder.class, recentMessages) {
            @Override
            public void populateViewHolder(ChatHolder chatView, Chat chat) {
                chatView.textView.setText(chat.getText());
//                chatView.textView.setPadding(30, 30, 30, 0);
//                chatView.nameView.setText(chat.getName());
//                chatView.nameView.setPadding(30, 0, 30, 30);
//                chatView.textView.setTextColor(Color.parseColor("#000000"));
//                chatView.textView.setTypeface(null, Typeface.NORMAL);
                if (mAuthData != null && chat.getUid().equals(mAuthData.getUid())) {
//                    chatView.textView.setGravity(Gravity.END);
//                    chatView.nameView.setGravity(Gravity.END);
//                    chatView.nameView.setTextColor(Color.parseColor("#AAAAAA"));
//                    chatView.itemView.setBackground(getDrawable(R.drawable.outgoing_message));
                } else {
//                    chatView.nameView.setTextColor(Color.parseColor("#00BCD4"));
//                    chatView.itemView.setBackground(getDrawable(R.drawable.incoming_message));
                }
            }
        };

        messages.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_login_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.login_menu_item).setVisible(mAuthData == null);
        menu.findItem(R.id.logout_menu_item).setVisible(mAuthData != null);
        mSendButton.setEnabled(mAuthData != null);
        mMessageEdit.setEnabled(mAuthData != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_menu_item:
                this.showFirebaseLoginPrompt();
                return true;
            case R.id.logout_menu_item:
                this.logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    // Start of FirebaseLoginBaseActivity

    @Override
    public void onFirebaseLoginSuccess(AuthData authData) {
        Log.i(TAG, "Logged in to " + authData.getProvider().toString());
        mAuthData = authData;

        switch (mAuthData.getProvider()) {
            case "password":
                name = (String) mAuthData.getProviderData().get("email");
                break;
            default:
                name = (String) mAuthData.getProviderData().get("displayName");
                break;
        }

        updateChat();
        invalidateOptionsMenu();
    }

    @Override
    public void onFirebaseLogout() {
        Log.i(TAG, "Logged out");
        mAuthData = null;
        name = "";
        invalidateOptionsMenu();
        updateChat();
    }

    @Override
    public void onFirebaseLoginProviderError(FirebaseError firebaseError) {
        Log.i(TAG, "Login provider error: " + firebaseError.toString());
    }

    @Override
    public void onFirebaseLoginUserError(FirebaseError firebaseError) {
        Log.i(TAG, "Login user error: " + firebaseError.toString());
    }

    @Override
    public Firebase getFirebaseRef() {
        return mRef;
    }

    // End of FirebaseLoginBaseActivity

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
        TextView nameView, textView;
        View itemView;

        public ChatHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            //nameView = (TextView) itemView.findViewById(android.R.id.text2);
            textView = (TextView) itemView.findViewById(R.id.message_text);
        }
    }
}
