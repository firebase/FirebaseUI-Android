/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.email;

import android.view.View;
import android.widget.ImageView;

class ImageFocusTransparencyChanger implements View.OnFocusChangeListener {
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
