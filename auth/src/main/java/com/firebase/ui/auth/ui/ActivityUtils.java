package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ActivityUtils {

    public static void finishActivity(Activity activity, int resultCode, Intent intent) {
        activity.setResult(resultCode, intent);
        activity.finish();
    }

}
