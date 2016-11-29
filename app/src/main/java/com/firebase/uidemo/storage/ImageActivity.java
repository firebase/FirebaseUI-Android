package com.firebase.uidemo.storage;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.firebase.uidemo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ImageActivity extends AppCompatActivity {

    private static final String TAG = "ImageDemo";
    private static final int RC_CHOOSE_PHOTO = 101;
    private static final int RC_IMAGE_PERMS = 102;

    private StorageReference mImageRef;

    @BindView(R.id.button_choose_photo)
    Button mUploadButton;

    @BindView(R.id.button_download_direct)
    Button mDownloadDirectButton;

    @BindView(R.id.first_image)
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        ButterKnife.bind(this);

        // By default, Firebase Storage files require authentication to read or write.
        // For this sample to function correctly, enable Anonymous Auth in the Firebase console:
        // https://console.firebase.google.com/project/_/authentication/providers
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInAnonymously:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInAnonymously", task.getException());
                            Log.w(TAG, getString(R.string.anonymous_auth_failed_msg));

                            Toast.makeText(ImageActivity.this,
                                    getString(R.string.anonymous_auth_failed_toast),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CHOOSE_PHOTO) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                uploadPhoto(selectedImage);
            } else {
                Toast.makeText(this, "No image chosen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @OnClick(R.id.button_choose_photo)
    @AfterPermissionGranted(RC_IMAGE_PERMS)
    protected void choosePhoto() {
        String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this, getString(R.string.rational_image_perm),
                    RC_IMAGE_PERMS, perm);
            return;
        }

        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RC_CHOOSE_PHOTO);
    }

    protected void uploadPhoto(Uri uri) {
        // Reset UI
        hideDownloadUI();
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        // Upload to Firebase Storage
        String uuid = UUID.randomUUID().toString();
        mImageRef = FirebaseStorage.getInstance().getReference(uuid);
        mImageRef.putFile(uri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "uploadPhoto:onSuccess:" +
                                taskSnapshot.getMetadata().getReference().getPath());
                        Toast.makeText(ImageActivity.this, "Image uploaded",
                                Toast.LENGTH_SHORT).show();

                        showDownloadUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "uploadPhoto:onError", e);
                        Toast.makeText(ImageActivity.this, "Upload failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @OnClick(R.id.button_download_direct)
    protected void downloadDirect() {
        // Download directly from StorageReference using Glide
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(mImageRef)
                .centerCrop()
                .crossFade()
                .into(mImageView);
    }

    private void hideDownloadUI() {
        mDownloadDirectButton.setEnabled(false);

        mImageView.setImageResource(0);
        mImageView.setVisibility(View.INVISIBLE);
    }

    private void showDownloadUI() {
        mDownloadDirectButton.setEnabled(true);

        mImageView.setVisibility(View.VISIBLE);
    }
}
