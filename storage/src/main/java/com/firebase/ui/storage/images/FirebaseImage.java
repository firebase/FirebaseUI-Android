/*
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firebase.ui.storage.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.storage.FileInfo;
import com.firebase.ui.storage.FirebaseFile;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;

/**
 * Helper class for manipulating images in Firebase Storage while keeping associated metadata
 * tied to records in Firebase Database.
 */
public class FirebaseImage {

    private static final String TAG = "FirebaseImage";

    /**
     * Get a BaseDownloader instance to begin downloading and displaying an image.
     * @param context context in which to perform the download.
     * @param reference the DatabaseReference to which the file was attached.
     * @return a BaseDownloader instance.
     */
    public static FirebaseImage.Downloader download(Context context, DatabaseReference reference) {
        return new FirebaseImage.Downloader(context, reference);
    }

    @Nullable
    @WorkerThread
    public static ImageInfo getImageInfo(FirebaseFile.Uploader uploader) {
        try {
            // Read the Bitmap from the Uploader
            final Bitmap bitmap;
            if (uploader.getUri() != null) {
                // Decode from Uri
                bitmap = MediaStore.Images.Media.getBitmap(
                        uploader.getContext().getContentResolver(), uploader.getUri());
            } else {
                // Decode from Bytes
                bitmap = BitmapFactory.decodeByteArray(uploader.getBytes(), 0,
                        uploader.getBytes().length);
            }

            // Generate a Palette
            Palette palette = new Palette.Builder(bitmap).generate();

            // Create ImageInfo and attach to Firebase record (synchronously)
            return new ImageInfo(bitmap, palette);
        } catch (Exception e) {
            Log.e(TAG, "Error in attaching imageInfo", e);
        }

        return null;
    }

    /**
     * Downloads and displays a previously uploaded file. The image metadata is used to display
     * a colored and sized preview of the image while the image download is ongoing.
     */
    public static class Downloader extends FirebaseFile.BaseDownloader {

        private static final String TAG = "FBImage.Downloader";

        private static final int VIBRANT = 1601;
        private static final int MUTED = 1602;

        private static final int MATCH_WIDTH = 1603;
        private static final int MATCH_HEIGHT = 1604;

        private ImageView mImageView;
        private int mPlaceholder = VIBRANT;
        private int mMatchDimension = -1;

        private Downloader(Context context, DatabaseReference reference) {
            super(context, reference);
        }

        /**
         * Specify the target ImageView in which the image should be displayed.
         * @param imageView the target view.
         * @return this downloader, for chaining.
         */
        public Downloader into(ImageView imageView) {
            this.mImageView = imageView;
            return this;
        }

        /**
         * Use a vibrant color from the image palette for the placeholder (Default).
         * @return this downloader, for chaining.
         */
        public Downloader vibrant() {
            mPlaceholder = VIBRANT;
            return this;
        }

        /**
         * Use a muted color from the image palette for the placeholder.
         * @return this downloader, for chaining.
         */
        public Downloader muted() {
            mPlaceholder = MUTED;
            return this;
        }

        /**
         * Resize the image to match the width of the target ImageView. This may result in the
         * height of the target view being adjusted,
         * @return this downloader, for chaining.
         */
        public Downloader matchWidth() {
            mMatchDimension = MATCH_WIDTH;
            return this;
        }

        /**
         * Resize the image to match the height of the target ImageView. This may result in the
         * width of the target view being adjusted.
         * @return this downloader, for chaining.
         */
        public Downloader matchHeight() {
            mMatchDimension = MATCH_HEIGHT;
            return this;
        }

        @Override
        protected void processFileInfo(final FileInfo fileInfo) {
            // Check that we have a target ImageView.
            if (mImageView == null) {
                throw new IllegalArgumentException("Must call into() before calling execute()");
            }


            // Get Image info
            ImageInfo imageInfo = fileInfo.imageInfo;
            if (imageInfo == null) {
                reportError("Error: no imageInfo in fileInfo:" + fileInfo);
                return;
            }

            // Get Storage reference
            StorageReference ref = fileInfo.getReference();

            // Base Glide request
            DrawableRequestBuilder builder = Glide.with(getContext())
                    .using(new FirebaseImageLoader())
                    .load(ref)
                    .crossFade()
                    .listener(new SimpleGlideListener<StorageReference, GlideDrawable>() {
                        @Override
                        public void onReady() {
                            reportSuccess(fileInfo);
                        }

                        @Override
                        public void onException(Exception e) {
                            Log.e(TAG, "glide:onException:" + e);
                            reportError("Glide Exception: " + e.getMessage());
                        }
                    });

            // Width/height override
            if (mMatchDimension > 0) {
                // Get override dimensions
                ImageInfo override = getOverrideDimensions(imageInfo);
                Log.d(TAG, "override:" + override.width + ":" + override.height);

                // If not 0, set them
                if (override.width > 0 && override.height > 0) {
                    // Pre-size the imageView
                    mImageView.getLayoutParams().width = override.width;
                    mImageView.getLayoutParams().height = override.height;
                    mImageView.requestLayout();

                    // Set the Glide size override
                    builder.override(override.width, override.height);
                } else {
                    Log.w(TAG, "override w or h < 0");
                    Log.w(TAG, "imageView:" + mImageView.getWidth() + "x" + mImageView.getHeight());
                }
            }

            // Chose either vibrant or muted placeholder
            if (mPlaceholder == VIBRANT) {
                builder = builder.placeholder(new ColorDrawable(imageInfo.paletteInfo.getVibrantColor()));
            } else {
                builder = builder.placeholder(new ColorDrawable(imageInfo.paletteInfo.getMutedColor()));
            }

            // Check if we are using RatioImageView, automatically set ImageInfo
            if (mImageView instanceof RatioImageView) {
                ((RatioImageView) mImageView).setImageInfo(imageInfo);
            }

            // Do the image load
            builder.into(mImageView);
        }

        private ImageInfo getOverrideDimensions(ImageInfo info) {
            ImageInfo result = new ImageInfo();

            if (mMatchDimension == MATCH_WIDTH) {
                // Scale to fill the width of the image view
                float heightToWidthRatio = (float) info.height / info.width;
                result.width = mImageView.getWidth() - mImageView.getPaddingLeft() - mImageView.getPaddingRight();
                result.height = (int) (heightToWidthRatio * result.width);
            } else if (mMatchDimension == MATCH_HEIGHT) {
                // Scale to fill the height of the image view
                float widthToHeightRatio = (float) info.width / info.height;
                result.height = mImageView.getHeight() - mImageView.getPaddingTop() - mImageView.getPaddingBottom();
                result.width = (int) (widthToHeightRatio * result.height);
            }

            return result;
        }

    }

    /**
     * Class to simplify listening to a Glide load.
     */
    private static abstract class SimpleGlideListener<M, T> implements RequestListener<M, T> {

        public abstract void onReady();
        public abstract void onException(Exception e);

        @Override
        public boolean onException(Exception e, M model, Target<T> target,
                                   boolean isFirstResource) {
            onException(e);
            return false;
        }

        @Override
        public boolean onResourceReady(T resource, M model, Target<T> target,
                                       boolean isFromMemoryCache, boolean isFirstResource) {
            onReady();
            return false;
        }
    }
}
