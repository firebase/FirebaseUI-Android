package com.firebase.ui.auth.ui.email;

import android.view.View;
import android.widget.ImageView;

/**
 * Created by serikb on 5/5/16.
 */
public class ImageFocusTransparencyChanger implements View.OnFocusChangeListener {
    private final ImageView mTogglePasswordImage;
    private final float mSlightlyVisible;
    private final float mVisible;

    public ImageFocusTransparencyChanger(
            ImageView togglePasswordImage,
            float visible,
            float slightlyVisible) {
        mTogglePasswordImage = togglePasswordImage;
        mVisible = visible;
        mSlightlyVisible = slightlyVisible;
        mTogglePasswordImage.setAlpha(mSlightlyVisible);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            mTogglePasswordImage.setAlpha(mVisible);
        } else {
            mTogglePasswordImage.setAlpha(mSlightlyVisible);
        }

    }
}
