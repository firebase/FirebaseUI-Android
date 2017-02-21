package com.firebase.uidemo.database;

import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.uidemo.R;

public class ChatHolder extends RecyclerView.ViewHolder {
    private final TextView mNameField;
    private final TextView mTextField;
    private final FrameLayout mLeftArrow;
    private final FrameLayout mRightArrow;
    private final RelativeLayout mMessageContainer;
    private final LinearLayout mMessage;
    private final int mGreen300;
    private final int mGray300;

    public ChatHolder(View itemView) {
        super(itemView);
        mNameField = (TextView) itemView.findViewById(R.id.name_text);
        mTextField = (TextView) itemView.findViewById(R.id.message_text);
        mLeftArrow = (FrameLayout) itemView.findViewById(R.id.left_arrow);
        mRightArrow = (FrameLayout) itemView.findViewById(R.id.right_arrow);
        mMessageContainer = (RelativeLayout) itemView.findViewById(R.id.message_container);
        mMessage = (LinearLayout) itemView.findViewById(R.id.message);
        mGreen300 = ContextCompat.getColor(itemView.getContext(), R.color.material_green_300);
        mGray300 = ContextCompat.getColor(itemView.getContext(), R.color.material_gray_300);
    }

    public void setIsSender(boolean isSender) {
        final int color;
        if (isSender) {
            color = mGreen300;
            mLeftArrow.setVisibility(View.GONE);
            mRightArrow.setVisibility(View.VISIBLE);
            mMessageContainer.setGravity(Gravity.END);
        } else {
            color = mGray300;
            mLeftArrow.setVisibility(View.VISIBLE);
            mRightArrow.setVisibility(View.GONE);
            mMessageContainer.setGravity(Gravity.START);
        }

        ((GradientDrawable) mMessage.getBackground()).setColor(color);
        ((RotateDrawable) mLeftArrow.getBackground()).getDrawable()
                .setColorFilter(color, PorterDuff.Mode.SRC);
        ((RotateDrawable) mRightArrow.getBackground()).getDrawable()
                .setColorFilter(color, PorterDuff.Mode.SRC);
    }

    public void setName(String name) {
        mNameField.setText(name);
    }

    public void setText(String text) {
        mTextField.setText(text);
    }
}
