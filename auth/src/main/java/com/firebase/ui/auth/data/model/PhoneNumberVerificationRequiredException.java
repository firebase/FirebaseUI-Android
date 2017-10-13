package com.firebase.ui.auth.data.model;

public class PhoneNumberVerificationRequiredException extends UnknownErrorException {
    private final String mPhoneNumber;

    public PhoneNumberVerificationRequiredException(String number) {
        super("Phone number requires verification.");
        mPhoneNumber = number;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }
}
