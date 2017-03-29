package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.UserProfileChangeRequest;

public class EditDisplayNameActivity extends SaveFieldActivity {
    private static final String TAG = "EditDisplayNameAct";
    private EditText mDisplayName;

    public static Intent createIntent(Context context, FlowParameters flowParameters) {
        return BaseHelper.createBaseIntent(context, EditDisplayNameActivity.class, flowParameters);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_display_name);
        mDisplayName = (EditText) findViewById(R.id.display_name);
        mDisplayName.setText(mActivityHelper.getCurrentUser().getDisplayName());

        displayProfilePicture();

        TextView removeLink = (TextView) findViewById(R.id.remove_profile_image_link);
        removeLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeProfilePicture();
            }
        });
    }

    @Override
    protected void onSaveMenuItem() {
        mActivityHelper.getCurrentUser().updateProfile(
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(mDisplayName.getText().toString())
                        .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        finish();
                    }
                })
                .addOnFailureListener(new TaskFailureLogger(TAG, "Failed to save display name"));
    }

    private void displayProfilePicture() {
        ImageView profilePicture = (ImageView) findViewById(R.id.profile_image);
        Uri profilePhoto = mActivityHelper.getCurrentUser().getPhotoUrl();
        Glide.with(this)
                .load(profilePhoto)
                .placeholder(R.drawable.ic_person_white_48dp)
                .transform(new CircleTransform(getApplicationContext()))
                .into(profilePicture);
    }

    private void removeProfilePicture() {
        mActivityHelper.getCurrentUser().updateProfile(
                new UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build())
        .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e(TAG, "successfully removed profile image");
                displayProfilePicture();
            }
        })
        .addOnFailureListener(new TaskFailureLogger(TAG, "unable to remove profile image"));
    }
}
