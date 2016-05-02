package com.firebase.ui.auth.ui.idp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.choreographer.ControllerConstants;
import com.firebase.ui.auth.choreographer.idp.IDPController;
import com.firebase.ui.auth.choreographer.idp.provider.FacebookProvider;
import com.firebase.ui.auth.choreographer.idp.provider.GoogleProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProvider;
import com.firebase.ui.auth.choreographer.idp.provider.IDPProviderParcel;
import com.firebase.ui.auth.choreographer.idp.provider.IDPResponse;
import com.google.firebase.auth.FacebookAuthProvider;
import com.firebase.ui.auth.R;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

public class AuthMethodPickerActivity
        extends IDPBaseActivity
        implements IDPProvider.IDPCallback, View.OnClickListener {

    private static final int NASCAR_BUTTON_PADDING = 36;
    private static final String TAG = "AuthMethodPicker";
    private ArrayList<IDPProviderParcel> mProviderParcels;
    private ArrayList<IDPProvider> mIDPProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nascar_layout);
        Button emailButton = (Button) findViewById(R.id.email_provider);
        emailButton.setOnClickListener(this);
    mProviderParcels = getIntent().getParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS);
        populateIDPList(mProviderParcels);
    }

    public static Intent createIntent(
            Context context, String appName, ArrayList<IDPProviderParcel> parcels) {
    return new Intent()
        .setClass(context, AuthMethodPickerActivity.class)
        .putExtra(ControllerConstants.EXTRA_APP_NAME, appName)
        .putParcelableArrayListExtra(ControllerConstants.EXTRA_PROVIDERS, parcels)
        .putExtra(EXTRA_ID, IDPController.NASCAR_SCREEN);
    }

    private void populateIDPList(ArrayList<IDPProviderParcel> mProviders) {
        mIDPProviders = new ArrayList<>();
        for (IDPProviderParcel providerParcel : mProviders) {
            switch (providerParcel.getProviderType()) {
                case FacebookAuthProvider.PROVIDER_ID :
                    mIDPProviders.add(
                            new FacebookProvider(
                                    this, providerParcel));
                    break;
                case GoogleAuthProvider.PROVIDER_ID:
                    mIDPProviders.add(new GoogleProvider(this, providerParcel));
                    break;
                default:
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Encountered unknown IDPProvider parcel with type: "
                                + providerParcel.getProviderType());
                    }
            }
        }
        LinearLayout btnHolder = (LinearLayout) findViewById(R.id.btn_holder);
        for(IDPProvider provider: mIDPProviders) {
            View loginButton = provider.getLoginButton(this);
            LinearLayout wrapper = new LinearLayout(getApplicationContext());
            wrapper.setPadding(0, NASCAR_BUTTON_PADDING, 0, 0);
            wrapper.addView(loginButton);
            provider.setAuthenticationCallback(this);
            btnHolder.addView(wrapper);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for(IDPProvider provider : mIDPProviders) {
           provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSuccess(IDPResponse response) {
        Intent data = new Intent();
        data.putExtra(ControllerConstants.EXTRA_IDP_RESPONSE, response);
        finish(RESULT_OK, data);
    }

    @Override
    public void onFailure(Bundle extra) {
        Intent data = new Intent();
        finish(LOGIN_CANCELLED, data);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.email_provider) {
           finish(IDPBaseActivity.EMAIL_LOGIN_NEEDED, new Intent());
        }
    }
}
