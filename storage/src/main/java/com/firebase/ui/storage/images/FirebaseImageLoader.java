package com.firebase.ui.storage.images;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * ModelLoader implementation to download images from FirebaseStorage with Glide.
 *
 * <p>
 * First, register this class in your AppGlideModule:
 * <pre>
 *         {@literal @}Override
 *         public void registerComponents(Context context, Registry registry) {
 *             // Register FirebaseImageLoader to handle StorageReference
 *             registry.append(StorageReference.class, InputStream.class,
 *                     new FirebaseImageLoader.Factory());
 *         }
 * </pre>
 *
 * <p>
 * Then load a StorageReference into an ImageView.
 * <pre>
 *     StorageReference ref = FirebaseStorage.getInstance().getReference().child("myimage");
 *     ImageView iv = (ImageView) findViewById(R.id.my_image_view);
 *
 *     GlideApp.with(this)
 *         .load(ref)
 *         .into(iv);
 * </pre>
 */
public class FirebaseImageLoader implements ModelLoader<StorageReference, InputStream> {

    private static final String TAG = "FirebaseImageLoader";


    /**
     * Factory to create {@link FirebaseImageLoader}.
     */
    public static class Factory implements ModelLoaderFactory<StorageReference, InputStream> {

        @NonNull
        @Override
        public ModelLoader<StorageReference, InputStream> build(@NonNull MultiModelLoaderFactory factory) {
            return new FirebaseImageLoader();
        }

        @Override
        public void teardown() {
            // No-op
        }
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull StorageReference reference,
                                               int height,
                                               int width,
                                               @NonNull Options options) {
        return new LoadData<>(
                new FirebaseStorageKey(reference),
                new FirebaseStorageFetcher(reference));
    }

    @Override
    public boolean handles(@NonNull StorageReference reference) {
        return true;
    }

    private static class FirebaseStorageKey implements Key {

        private StorageReference mRef;

        public FirebaseStorageKey(StorageReference ref) {
            mRef = ref;
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest digest) {
            digest.update(mRef.getPath().getBytes(Charset.defaultCharset()));
        }
    }

    private static class FirebaseStorageFetcher implements DataFetcher<InputStream> {

        private StorageReference mRef;
        private StreamDownloadTask mStreamTask;
        private InputStream mInputStream;

        public FirebaseStorageFetcher(StorageReference ref) {
            mRef = ref;
        }

        @Override
        public void loadData(@NonNull Priority priority,
                             @NonNull final DataCallback<? super InputStream> callback) {
            mStreamTask = mRef.getStream();
            mStreamTask
                    .addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(StreamDownloadTask.TaskSnapshot snapshot) {
                            mInputStream = snapshot.getStream();
                            callback.onDataReady(mInputStream);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callback.onLoadFailed(e);
                        }
                    });
        }

        @Override
        public void cleanup() {
            // Close stream if possible
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                    mInputStream = null;
                } catch (IOException e) {
                    Log.w(TAG, "Could not close stream", e);
                }
            }
        }

        @Override
        public void cancel() {
            // Cancel task if possible
            if (mStreamTask != null && mStreamTask.isInProgress()) {
                mStreamTask.cancel();
            }
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }
}
