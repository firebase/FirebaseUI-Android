package com.firebase.ui.auth.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.Preconditions.checkNotNull;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ActivityUtils {

    public static Intent createBaseIntent(
            @NonNull Context context,
            @NonNull Class<? extends Activity> target,
            @NonNull FlowParameters flowParams) {
        return new Intent(
                checkNotNull(context, "context cannot be null"),
                checkNotNull(target, "target activity cannot be null"))
                .putExtra(ExtraConstants.EXTRA_FLOW_PARAMS,
                          checkNotNull(flowParams, "flowParams cannot be null"));
    }

    public static void finishActivity(Activity activity, int resultCode, Intent intent) {
        activity.setResult(resultCode, intent);
        activity.finish();
    }

    public static void saveCredentialsOrFinish(
            @Nullable SaveSmartLock saveSmartLock,
            Activity activity,
            FirebaseUser firebaseUser,
            @Nullable String password,
            IdpResponse response) {
        if (saveSmartLock == null) {
            finishActivity(activity, ResultCodes.OK, response.toIntent());
        } else {
            saveSmartLock.saveCredentialsOrFinish(
                    firebaseUser,
                    password,
                    response);
        }
    }
}
