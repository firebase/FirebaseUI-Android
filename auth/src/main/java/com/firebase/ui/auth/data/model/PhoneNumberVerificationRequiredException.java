package com.firebase.ui.auth.data.model;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

/**
 * Represents an error in which the phone number couldn't be automatically verified and must
 * therefore be manually verified by the client by sending an SMS code.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PhoneNumberVerificationRequiredException extends FirebaseUiException {
    private final String mPhoneNumber;

    /**
     * @param number the phone number requiring verification, formatted with a country code prefix
     */
    public PhoneNumberVerificationRequiredException(@NonNull String number) {
        super(ErrorCodes.PROVIDER_ERROR, "Phone number requires verification.");
        mPhoneNumber = number;
    }

    /**
     * @return the phone number requiring verification
     */
    @NonNull
    public String getPhoneNumber() {
        return mPhoneNumber;
    }
}
