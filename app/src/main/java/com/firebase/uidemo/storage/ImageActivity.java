package com.firebase.uidemo.storage;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.firebase.uidemo.BuildConfig;
import com.firebase.uidemo.R;
import com.firebase.uidemo.util.SignInResultNotifier;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
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

    @BindView(R.id.button_choose_photo)
    Button mUploadButton;

    @BindView(R.id.button_download_direct)
    Button mDownloadDirectButton;

    @BindView(R.id.first_image)
    ImageView mImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        ButterKnife.bind(this);

        // By default, Cloud Storage files require authentication to read or write.
        // For this sample to function correctly, enable Anonymous Auth in the Firebase console:
        // https://console.firebase.google.com/project/_/authentication/providers
        FirebaseAuth.getInstance()
                .signInAnonymously()
                .addOnCompleteListener(new SignInResultNotifier(this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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

    private void uploadPhoto(Uri uri) {
        // Reset UI
        hideDownloadUI();
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        // Upload to Cloud Storage
        String uuid = UUID.randomUUID().toString();
        mImageRef = FirebaseStorage.getInstance().getReference(uuid);
        mImageRef.putFile(uri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "uploadPhoto:onSuccess:" +
                                    taskSnapshot.getMetadata().getReference().getPath());
                        }
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
        // (See MyAppGlideModule for Loader registration)
        GlideApp.with(this)
                .load(mImageRef)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // See #choosePhoto with @AfterPermissionGranted
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this,
                                                            Collections.singletonList(PERMS))) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}
