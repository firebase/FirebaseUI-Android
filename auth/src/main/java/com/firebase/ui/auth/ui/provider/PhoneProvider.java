package com.firebase.ui.auth.ui.provider;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.ui.phone.PhoneActivity;
import com.firebase.ui.auth.viewmodel.RequestCodes;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneProvider extends ProviderBase {
    private final MutableLiveData<IdpResponse> mResponseData = new MutableLiveData<>();
    private final AuthUI.IdpConfig mConfig;

    public PhoneProvider(AuthUI.IdpConfig config) {
        mConfig = config;
    }

    @Override
    public LiveData<IdpResponse> getResponseListener() {
        return mResponseData;
    }

    @StringRes
    @Override
    public int getNameRes() {
        return R.string.fui_provider_name_phone;
    }

    @Override
    @LayoutRes
    public int getButtonLayout() {
        return R.layout.fui_provider_button_phone;
    }

    @Override
    public void startLogin(@NonNull HelperActivityBase activity) {
        activity.startActivityForResult(
                PhoneActivity.createIntent(
                        activity, activity.getFlowParams(), mConfig.getParams()),
                RequestCodes.PHONE_FLOW);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RequestCodes.PHONE_FLOW) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null) {
                mResponseData.setValue(IdpResponse.fromError(
                        new UserCancellationException()));
            } else {
                mResponseData.setValue(response);
            }
        }
    }
}
