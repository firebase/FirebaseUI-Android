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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.firebase.ui.storage.R;

/**
 * ImageView that matches the aspect ratio from an {@link ImageInfo} object.
 */
public class RatioImageView extends ImageView {

    private static final String TAG = "RatioImageView";

    private static final int FIXED_WIDTH = 0;
    private static final int FIXED_HEIGHT = 1;

    private static final int DEFAULT_FIXED_DIMENSION = FIXED_WIDTH;
    private static final float DEFAULT_ASPECT_RATIO = 1.0f;

    private ImageInfo mImageInfo;
    private int mFixedDimension;

    public RatioImageView(Context context) {
        super(context);
    }

    public RatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RatioImageView);
        mFixedDimension = a.getInt(R.styleable.RatioImageView_fixed_dimension,
                DEFAULT_FIXED_DIMENSION);
        a.recycle();
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.mImageInfo = imageInfo;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get aspect ratio from ImageInfo
        float aspectRatio = DEFAULT_ASPECT_RATIO;
        if (mImageInfo != null) {
            aspectRatio = ((float) mImageInfo.width / mImageInfo.height); // w : h
        }

        // Set dimensions according to aspect ratio
        if (mFixedDimension == FIXED_WIDTH) {
            int width = getMeasuredWidth();
            int height = (int) (width / aspectRatio);

            setMeasuredDimension(width, height);
        } else if (mFixedDimension == FIXED_HEIGHT) {
            int height = getMeasuredHeight();
            int width = (int) (height * aspectRatio);

            setMeasuredDimension(width, height);
        } else {
            Log.w(TAG, "Invalid fixed_dimension attribute, must be 'width' or 'height'.");
        }
    }
}
