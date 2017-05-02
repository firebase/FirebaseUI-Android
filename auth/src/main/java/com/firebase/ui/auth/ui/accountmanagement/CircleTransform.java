package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

/**
 * TODO javadoc
 */
class CircleTransform extends BitmapTransformation {
    CircleTransform(Context context) {
        super(context);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap source, int width, int height) {
        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(
                source,
                BitmapShader.TileMode.CLAMP,
                BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return result;
    }

    @Override
    public String getId() {
        return "CircleTransform";
    }
}
