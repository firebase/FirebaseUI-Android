package com.firebase.ui.auth.data.model;

/**
 * Represents an error in which the phone number couldn't be automatically verified and must
 * therefore be manually verified by the client by sending an SMS code.
 */
public class PhoneNumberVerificationRequiredException extends UnknownErrorException {
    private final String mPhoneNumber;

    /**
     * @param number the phone number requiring verification, formatted with a country code prefix
     */
    public PhoneNumberVerificationRequiredException(String number) {
        super("Phone number requires verification.");
        mPhoneNumber = number;
    }

    /**
     * @return the phone number requiring verification
     */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }
}