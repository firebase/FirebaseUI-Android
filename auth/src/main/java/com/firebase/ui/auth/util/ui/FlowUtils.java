/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.util.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.IntentRequiredException;
import com.firebase.ui.auth.data.model.PendingIntentRequiredException;
import com.firebase.ui.auth.ui.FragmentBase;
import com.firebase.ui.auth.ui.HelperActivityBase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
