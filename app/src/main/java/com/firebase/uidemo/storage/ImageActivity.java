package com.firebase.uidemo.storage;

import android.Manifest;
import android.app.Activity;
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
import com.firebase.ui.storage.DownloadResult;
import com.firebase.ui.storage.FirebaseFile;
import com.firebase.ui.storage.UploadResult;
import com.firebase.ui.storage.images.FirebaseImage;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.firebase.uidemo.R;
import com.firebase.uidemo.util.SignInResultNotifier;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class ImageActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "ImageDemo";
    private static final int RC_CHOOSE_PHOTO = 101;
    private static final int RC_IMAGE_PERMS = 102;
    private static final String PERMS = Manifest.permission.READ_EXTERNAL_STORAGE;

    private StorageReference mImageRef;

    private DatabaseReference mDatabase;

    @BindView(R.id.button_choose_photo)
    Button mUploadButton;

    @BindView(R.id.button_download_indirect)
    Button mDownloadIndirectButton;

    @BindView(R.id.button_download_direct)
    Button mDownloadDirectButton;

    @BindView(R.id.button_download_bytes)
    Button mDownloadBytesButton;

    @BindView(R.id.first_image)
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        ButterKnife.bind(this);

        // By default, Cloud Storage files require authentication to read or write.
        // For this sample to function correctly, enable Anonymous Auth in the Firebase console:
        // https://console.firebase.google.com/project/_/authentication/providers

        // Initialize Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Authenticate so that uploading/downloading works
        FirebaseAuth.getInstance()
                .signInAnonymously()
                .addOnCompleteListener(new SignInResultNotifier(this));
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
        } else if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                && EasyPermissions.hasPermissions(this, PERMS)) {
            choosePhoto();
        }
    }

    @OnClick(R.id.button_choose_photo)
    @AfterPermissionGranted(RC_IMAGE_PERMS)
    protected void choosePhoto() {
        if (!EasyPermissions.hasPermissions(this, PERMS)) {
            EasyPermissions.requestPermissions(this, getString(R.string.rational_image_perm),
                                               RC_IMAGE_PERMS, PERMS);
            return;
        }

        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RC_CHOOSE_PHOTO);
    }

    protected void uploadPhoto(Uri uri) {
        // Reset UI
        hideDownloadUI();

        // Ref to a random object where we will upload/download an image
        mDatabase = mDatabase.child("imagedemo").push();

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        FirebaseFile.upload(this, mDatabase)
                .from(uri)
                .fileType(FirebaseFile.TYPE_IMAGE)
                .execute()
                .addOnSuccessListener(this, new OnSuccessListener<UploadResult>() {
                    @Override
                    public void onSuccess(UploadResult uploadResult) {
                        Log.d(TAG, "uploadPhoto:onSuccess:" + uploadResult.getInfo().storagePath);
                        Toast.makeText(ImageActivity.this, "Image uploaded",
                                       Toast.LENGTH_SHORT).show();

                        mImageRef = uploadResult.getInfo().getReference();
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

    @OnClick(R.id.button_download_indirect)
    protected void downloadIndirect() {
        // Download from DatabaseReference using FirebaseImage
        FirebaseImage.download(this, mDatabase)
                .into(mImageView)
                .vibrant()
                .execute()
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "downloadPhoto:onError", e);
                        Toast.makeText(ImageActivity.this, "Download failed",
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

    @OnClick(R.id.button_download_bytes)
    protected void downloadBytes() {
        // Download as bytes
        final Activity activity = this;
        FirebaseFile.downloadBytes(this, mDatabase).execute()
                .addOnSuccessListener(new OnSuccessListener<DownloadResult>() {
                    @Override
                    public void onSuccess(DownloadResult result) {
                        byte[] bytes = result.getBytes();
                        Toast.makeText(activity, "Downloaded bytes: " + bytes.length,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(activity, "Error downloading bytes",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hideDownloadUI() {
        mDownloadIndirectButton.setEnabled(false);
        mDownloadDirectButton.setEnabled(false);
        mDownloadBytesButton.setEnabled(false);

        mImageView.setImageResource(0);
        mImageView.setVisibility(View.INVISIBLE);
    }

    private void showDownloadUI() {
        mDownloadIndirectButton.setEnabled(true);
        mDownloadDirectButton.setEnabled(true);
        mDownloadBytesButton.setEnabled(true);

        mImageView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        // See #choosePhoto with @AfterPermissionGranted
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this,
                                                            Collections.singletonList(PERMS))) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}
