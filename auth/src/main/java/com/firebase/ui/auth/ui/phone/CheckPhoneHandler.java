package com.firebase.ui.auth.ui.phone;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.PhoneNumber;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.firebase.ui.auth.viewmodel.RequestCodes;
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest;
import com.google.android.gms.auth.api.identity.Identity;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPhoneHandler extends AuthViewModelBase<PhoneNumber> {

    private static final String TAG = "CheckPhoneHandler";

    public CheckPhoneHandler(Application application) {
        super(application);
    }

    /**
     * Initiates the Phone Number Hint flow using the new API.
     *
     * <p>This method creates a GetPhoneNumberHintIntentRequest and calls
     * Identity.getSignInClient(activity).getPhoneNumberHintIntent(request) to retrieve an
     * IntentSender. The IntentSender is then wrapped in a PendingIntentRequiredException so that
     * the caller can launch the hint flow.
     *
     * <p><strong>Note:</strong> Update your PendingIntentRequiredException to accept an IntentSender
     * rather than a PendingIntent.
     *
     * @param activity The activity used to retrieve the Phone Number Hint IntentSender.
     */
    public void fetchCredential(final Activity activity) {
        GetPhoneNumberHintIntentRequest request = GetPhoneNumberHintIntentRequest.builder().build();
        Identity.getSignInClient(activity)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener(result -> {
                    try {
                        // The new API returns an IntentSender.
                        IntentSender intentSender = result.getIntentSender();
                        // Update your exception to accept an IntentSender.
                        setResult(Resource.forFailure(new PendingIntentRequiredException(intentSender, RequestCodes.CRED_HINT)));
                    } catch (Exception e) {
                        Log.e(TAG, "Launching the IntentSender failed", e);
                        setResult(Resource.forFailure(e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Phone Number Hint failed", e);
                    setResult(Resource.forFailure(e));
                });
    }

    /**
     * Handles the result from the Phone Number Hint flow.
     *
     * <p>Call this method from your Activity's onActivityResult. It extracts the phone number from the
     * returned Intent and formats it.
     *
     * @param activity    The activity used to process the returned Intent.
     * @param requestCode The request code (should match RequestCodes.CRED_HINT).
     * @param resultCode  The result code from the hint flow.
     * @param data        The Intent data returned from the hint flow.
     */
    public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != RequestCodes.CRED_HINT || resultCode != Activity.RESULT_OK) {
            return;
        }
        try {
            String phoneNumber = Identity.getSignInClient(activity).getPhoneNumberFromIntent(data);
            String formattedPhone = PhoneNumberUtils.formatUsingCurrentCountry(phoneNumber, getApplication());
            if (formattedPhone != null) {
                setResult(Resource.forSuccess(PhoneNumberUtils.getPhoneNumber(formattedPhone)));
            } else {
                setResult(Resource.forFailure(new Exception("Failed to format phone number")));
            }
        } catch (Exception e) {
            Log.e(TAG, "Phone Number Hint failed", e);
            setResult(Resource.forFailure(e));
        }
    }
}