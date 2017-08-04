package com.firebase.uidemo.firestore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.uidemo.R;
import com.firebase.uidemo.database.ChatHolder;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * TODO
 */
public class FirestoreChatActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    private static final String TAG = "FirestoreChat";

    @BindView(R.id.messagesList)
    RecyclerView mRecycler;

    @BindView(R.id.sendButton)
    Button mSendButton;

    @BindView(R.id.messageEdit)
    EditText mMessageEdit;

    @BindView(R.id.emptyTextView)
    TextView mEmptyListMessage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirestoreRecyclerAdapter<Chat, ChatHolder> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        // Enable verbose Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Get the last 50 chat messages, ordered by timestamp
        Query query = mFirestore.collection("chats").orderBy("timestamp").limit(50);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        mAdapter = new FirestoreRecyclerAdapter<Chat, ChatHolder>(query, Chat.class) {
            @Override
            public void onBindViewHolder(ChatHolder holder, int i, Chat model) {
                holder.bind(model);
            }

            @Override
            public ChatHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext())
                        .inflate(R.layout.message, group, false);

                return new ChatHolder(view);
            }

            @Override
            public void onDataChanged() {
                // If there are no chat messages, show a view that invites the user to add a message.
                mEmptyListMessage.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };

        mRecycler.setLayoutManager(manager);
        mRecycler.setAdapter(mAdapter);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        if (auth.getCurrentUser() != null) {
            mAdapter.startListening();
            mSendButton.setEnabled(true);
        } else {
            mAdapter.stopListening();
            mSendButton.setEnabled(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        signInAnonymously();
        mAuth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        mAuth.removeAuthStateListener(this);
        mAdapter.stopListening();
    }

    private void signInAnonymously() {
        if (mAuth.getCurrentUser() != null) {
            return;
        }

        mAuth.signInAnonymously()
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "signIn:failure", e);
                        Toast.makeText(FirestoreChatActivity.this,
                                "Authentication failed.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @OnClick(R.id.sendButton)
    public void onSendClick() {
        String uid = mAuth.getCurrentUser().getUid();
        String name = "User " + uid.substring(0, 6);

        Chat chat = new Chat(name, mMessageEdit.getText().toString(), uid);

        mFirestore.collection("chats").add(chat)
                .addOnCompleteListener(this,
                        new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                if (!task.isSuccessful()) {
                                    Log.e(TAG, "Failed to write message", task.getException());
                                }
                            }
                        });

        mMessageEdit.setText("");
    }
}
