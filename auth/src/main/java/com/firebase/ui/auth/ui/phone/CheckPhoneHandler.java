package com.firebase.ui.auth.ui.phone;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPhoneHandler extends AuthViewModelBase<PhoneNumber> {
    public CheckPhoneHandler(Application application) {
        super(application);
    }


    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) { return; }

//        Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
//        String formattedPhone = PhoneNumberUtils.formatUsingCurrentCountry(
//                credential.getId(), getApplication());
//        if (formattedPhone != null) {
//            setResult(Resource.forSuccess(PhoneNumberUtils.getPhoneNumber(formattedPhone)));
//        }
    }
}
