package com.firebase.ui.auth.ui.idp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import com.firebase.ui.auth.R;

public class SupportVectorDrawablesButton extends AppCompatButton {
    public SupportVectorDrawablesButton(Context context) {
        super(context);
    }

    public SupportVectorDrawablesButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSupportVectorDrawablesAttrs(attrs);
    }

    public SupportVectorDrawablesButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSupportVectorDrawablesAttrs(attrs);
    }

    private void initSupportVectorDrawablesAttrs(AttributeSet attrs) {
        if (attrs == null) { return; }

        TypedArray attributeArray = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.SupportVectorDrawablesButton);

        Drawable drawableStart = null;
        Drawable drawableEnd = null;
        Drawable drawableBottom = null;
        Drawable drawableTop = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawableStart = attributeArray.getDrawable(
                    R.styleable.SupportVectorDrawablesButton_drawableStartCompat);
            drawableEnd = attributeArray.getDrawable(
                    R.styleable.SupportVectorDrawablesButton_drawableEndCompat);
            drawableBottom = attributeArray.getDrawable(
                    R.styleable.SupportVectorDrawablesButton_drawableBottomCompat);
            drawableTop = attributeArray.getDrawable(
                    R.styleable.SupportVectorDrawablesButton_drawableTopCompat);
        } else {
            int drawableStartId = attributeArray.getResourceId(
                    R.styleable.SupportVectorDrawablesButton_drawableStartCompat, -1);
            int drawableEndId = attributeArray.getResourceId(
                    R.styleable.SupportVectorDrawablesButton_drawableEndCompat, -1);
            int drawableBottomId = attributeArray.getResourceId(
                    R.styleable.SupportVectorDrawablesButton_drawableBottomCompat, -1);
            int drawableTopId = attributeArray.getResourceId(
                    R.styleable.SupportVectorDrawablesButton_drawableTopCompat, -1);

            if (drawableStartId != -1) {
                drawableStart = AppCompatResources.getDrawable(getContext(), drawableStartId);
            }
            if (drawableEndId != -1) {
                drawableEnd = AppCompatResources.getDrawable(getContext(), drawableEndId);
            }
            if (drawableBottomId != -1) {
                drawableBottom = AppCompatResources.getDrawable(getContext(), drawableBottomId);
            }
            if (drawableTopId != -1) {
                drawableTop = AppCompatResources.getDrawable(getContext(), drawableTopId);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableStart, drawableTop, drawableEnd, drawableBottom);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(
                    drawableStart, drawableTop, drawableEnd, drawableBottom);
        }

        attributeArray.recycle();
    }
}
