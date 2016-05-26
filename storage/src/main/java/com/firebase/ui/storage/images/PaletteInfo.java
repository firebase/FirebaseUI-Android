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

import android.support.v7.graphics.Palette;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Stores palette information about an image.
 */
public class PaletteInfo implements Serializable {

    // TODO(samstern): Better defaults, at least different muted and vibrant
    private static final int DEFAULT_COLOR = 0;

    public int vibrant;
    public int darkVibrant;
    public int lightVibrant;
    public int muted;
    public int darkMuted;
    public int lightMuted;

    public PaletteInfo() {}

    public PaletteInfo(Palette palette) {
        // Vibrant colors
        vibrant = palette.getVibrantColor(DEFAULT_COLOR);
        darkVibrant = palette.getDarkVibrantColor(DEFAULT_COLOR);
        lightVibrant = palette.getLightVibrantColor(DEFAULT_COLOR);

        // Muted colors
        muted = palette.getMutedColor(DEFAULT_COLOR);
        darkMuted = palette.getDarkMutedColor(DEFAULT_COLOR);
        lightMuted = palette.getLightMutedColor(DEFAULT_COLOR);
    }

    @Exclude
    public int getVibrantColor() {
        if (vibrant != DEFAULT_COLOR) {
            return vibrant;
        } else if (darkVibrant != DEFAULT_COLOR) {
            return darkVibrant;
        } else if (lightVibrant != DEFAULT_COLOR) {
            return lightVibrant;
        }

        return DEFAULT_COLOR;
    }

    @Exclude
    public int getMutedColor() {
        if (muted != DEFAULT_COLOR) {
            return muted;
        } else if (darkMuted != DEFAULT_COLOR) {
            return darkMuted;
        } else if (lightMuted != DEFAULT_COLOR) {
            return lightMuted;
        }

        return DEFAULT_COLOR;
    }

    @Override
    public String toString() {
        return "{ " +
                "vibrant:" + getVibrantColor() +", " +
                "muted:" + getMutedColor() +
                " }";
    }

}
