package com.firebase.ui.auth.ui.accountmanagement;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.R;

/**
 * TODO javadoc
 */
public class ProviderView extends LinearLayout {
    private static final String TAG = "ProviderView";

    private String mProviderId;
    private ImageView mProviderImage;
    private TextView mDisplayName;
    private String mDisplayNameValue;
    private TextView mProviderName;
    private ImageButton mUnlinkButton;

    public ProviderView(Context context, AttributeSet attributes) {
        super(context, attributes);
        inflate(getContext(), R.layout.linked_provider_layout, this);

        TypedArray a = context
                .getTheme()
                .obtainStyledAttributes(attributes, R.styleable.ProviderView, 0, 0);
        mDisplayNameValue = a.getString(R.styleable.ProviderView_displayName);
        switch (a.getInt(R.styleable.ProviderView_provider, -1)) {
            case 0:
                mProviderId = AuthUI.GOOGLE_PROVIDER;
                break;
            case 1:
                mProviderId = AuthUI.FACEBOOK_PROVIDER;
                break;
            case 2:
                mProviderId = AuthUI.TWITTER_PROVIDER;
                break;
            case 3:
                mProviderId = AuthUI.EMAIL_PROVIDER;
                break;
            case -1:
            default:
                Log.e(TAG, "Provider ID not set");
        }
        populateView();
    }

    public void setProviderId(String providerId) {
        mProviderId = providerId;
        populateView();

    }

    public void setDisplayName(String displayName) {
        mDisplayNameValue = displayName;
        populateView();
    }


    private void populateView() {
        mProviderImage = (ImageView) findViewById(R.id.provider_image);
        mDisplayName = (TextView) findViewById(R.id.display_name);
        mProviderName = (TextView) findViewById(R.id.provider_name);
        mUnlinkButton = (ImageButton) findViewById(R.id.unlink_idp_button);
        mDisplayName.setText(mDisplayNameValue);
        int px;
        if (mProviderId == null) {
            return;
        }
        switch (mProviderId) {
            case AuthUI.EMAIL_PROVIDER:
                mProviderImage.setBackground(makeColoredCircle(R.color.gdi_email_provider_grey));
                mProviderImage.setImageResource(R.drawable.ic_email_white_48dp);
                mProviderName.setText(R.string.idp_name_email);
                break;
            case AuthUI.FACEBOOK_PROVIDER:
                px = pxFromDp(10);
                mProviderImage.setPadding(px, px, px, px);
                mProviderImage.setBackground(makeColoredCircle(R.color.gdi_facebook_blue));
                mProviderImage.setImageResource(R.drawable.ic_facebook_white_22dp);
                mProviderName.setText(R.string.idp_name_facebook);
                break;
            case AuthUI.GOOGLE_PROVIDER:
                mProviderImage.setBackground(
                        makeColoredCircle(R.color.gdi_white_background));
                mProviderImage.setImageResource(R.drawable.ic_googleg_color_24dp);
                mProviderName.setText(R.string.idp_name_google);
                break;
            case AuthUI.TWITTER_PROVIDER:
                mProviderImage.setBackground(makeColoredCircle(R.color.gdi_twitter_blue));
                mProviderImage.setImageResource(R.drawable.ic_twitter_bird_white_24dp);
                px = pxFromDp(10);
                mProviderImage.setPadding(px, px, px, px);
                mProviderName.setText(R.string.idp_name_twitter);
                break;
        }

    }

    public void setUnlinkListener(OnClickListener unlinkListener) {
        mUnlinkButton.setOnClickListener(unlinkListener);
    }

    private ShapeDrawable makeColoredCircle(@ColorRes int color) {
        ShapeDrawable shapeDrawable = new ShapeDrawable();
        shapeDrawable.setShape(new OvalShape());
        int resolvedColor = ContextCompat.getColor(getContext(), color);
        shapeDrawable.getPaint().setColor(resolvedColor);
        return shapeDrawable;
    }

    private int pxFromDp(int dp) {
        return (int) Math.ceil(dp * getResources().getDisplayMetrics().density);
    }
}
