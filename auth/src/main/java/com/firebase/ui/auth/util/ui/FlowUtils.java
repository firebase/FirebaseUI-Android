package com.firebase.ui.auth.util.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.HelperActivityBase;

public final class FlowUtils {
    private FlowUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean unhandled(@NonNull HelperActivityBase activity, @Nullable Exception e) {
        if (e instanceof IntentRequiredException) {
            IntentRequiredException typed = (IntentRequiredException) e;
            activity.startActivityForResult(typed.getIntent(), typed.getRequestCode());
            return false;
        } else if (e instanceof PendingIntentRequiredException) {
            PendingIntentRequiredException typed = (PendingIntentRequiredException) e;
            startIntentSenderForResult(activity, typed.getPendingIntent(), typed.getRequestCode());
            return false;
        }

        return true;
    }

    public static boolean unhandled(@NonNull FragmentBase fragment, @Nullable Exception e) {
        if (e instanceof IntentRequiredException) {
            IntentRequiredException typed = (IntentRequiredException) e;
            fragment.startActivityForResult(typed.getIntent(), typed.getRequestCode());
            return false;
        } else if (e instanceof PendingIntentRequiredException) {
            PendingIntentRequiredException typed = (PendingIntentRequiredException) e;
            startIntentSenderForResult(fragment, typed.getPendingIntent(), typed.getRequestCode());
            return false;
        }

        return true;
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

    private static void startIntentSenderForResult(FragmentBase fragment,
                                                   PendingIntent intent,
                                                   int requestCode) {
        try {
            fragment.startIntentSenderForResult(
                    intent.getIntentSender(), requestCode, null, 0, 0, 0, null);
        } catch (IntentSender.SendIntentException e) {
            HelperActivityBase activity = (HelperActivityBase) fragment.requireActivity();
            activity.finish(Activity.RESULT_CANCELED, IdpResponse.getErrorIntent(e));
        }
    }
}
