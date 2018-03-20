package com.firebase.ui.auth.util.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.data.model.UserCancellationException;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.HelperActivityBase;

public final class FlowUtils {
    private FlowUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean handleError(@NonNull HelperActivityBase activity, @Nullable Exception e) {
        if (e instanceof UserCancellationException) {
            activity.finish(Activity.RESULT_CANCELED, null);
            return true;
        } else if (e instanceof IntentRequiredException) {
            IntentRequiredException typed = (IntentRequiredException) e;
            activity.startActivityForResult(typed.getIntent(), typed.getRequestCode());
            return true;
        } else if (e instanceof PendingIntentRequiredException) {
            PendingIntentRequiredException typed = (PendingIntentRequiredException) e;
            startIntentSenderForResult(activity, typed.getPendingIntent(), typed.getRequestCode());
            return true;
        }

        return false;
    }

    public static boolean handleError(FragmentBase fragment, Exception e) {
        return handleError(((HelperActivityBase) fragment.getActivity()), e);
    }

    private static void startIntentSenderForResult(HelperActivityBase activity,
                                                   PendingIntent intent,
                                                   int requestCode) {
        try {
            activity.startIntentSenderForResult(
                    intent.getIntentSender(), requestCode, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            activity.finish(Activity.RESULT_CANCELED, IdpResponse.getErrorIntent(e));
        }
    }
}
