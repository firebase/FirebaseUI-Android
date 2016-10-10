package com.firebase.ui.storage.images;

import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;

import java.io.IOException;
import java.io.InputStream;

/**
 * ModelLoader implementation to download images from FirebaseStorage with Glide.
 *
 * Sample Usage:
 * <pre>
 *     StorageReference ref = FirebaseStorage.getInstance().getReference().child("myimage");
 *     ImageView iv = (ImageView) findViewById(R.id.my_image_view);
 *
 *     Glide.with(this)
 *         .using(new FirebaseImageLoader())
 *         .load(ref)
 *         .into(iv);
 * </pre>
 */
public class FirebaseImageLoader implements StreamModelLoader<StorageReference> {

    private static final String TAG = "FirebaseImageLoader";

    @Override
    public DataFetcher<InputStream> getResourceFetcher(StorageReference model, int width, int height) {
        return new FirebaseStorageFetcher(model);
    }

    private class FirebaseStorageFetcher implements DataFetcher<InputStream> {

        private StorageReference mRef;
        private StreamDownloadTask mStreamTask;
        private InputStream mInputStream;

        FirebaseStorageFetcher(StorageReference ref) {
            mRef = ref;
        }

        @Override
        public InputStream loadData(Priority priority) throws Exception {
            mStreamTask = mRef.getStream();
            mInputStream = Tasks.await(mStreamTask).getStream();

            return mInputStream;
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
        public String getId() {
            return mRef.getPath();
        }

        @Override
        public void cancel() {
            // Cancel task if possible
            if (mStreamTask != null && mStreamTask.isInProgress()) {
                mStreamTask.cancel();
            }
        }
    }
}
