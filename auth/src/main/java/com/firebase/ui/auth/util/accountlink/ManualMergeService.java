package com.firebase.ui.auth.util.accountlink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.Task;

/**
 * This service is to be used in conjunction with {@link AuthUI.SignInIntentBuilder#setIsAccountLinkingEnabled(boolean,
 * Class)}. See a detailed explanation on <a href="https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#handling-account-link-failures">how
 * to use this service</a>.
 */
public abstract class ManualMergeService extends Service {
    private final IBinder mBinder = new ManualMergeUtils.MergeBinder(this);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * During this phase of the login process, you should load any data to be transferred from the
     * current user to a new one created in the sign-in flow.
     * <p>
     * <i>Note:</i> this service will stay alive until {@link #onTransferData(IdpResponse)}
     * completes so you may store your data in a field.
     *
     * @return a task to load your user's data or null if you can load the data synchronously
     * without blocking the main thread. The login process will wait to create the new user until
     * this task completes thus allowing you to work around security rules.
     */
    @Nullable
    public abstract Task<Void> onLoadData();

    /**
     * During this phase of the login process, you should transfer the data you loaded in {@link
     * #onLoadData()} to the new user's account.
     *
     * @param response the new user's metadata
     * @return a task to transfer the old user's data to the new one or null if you don't need to
     * wait for the data to be transferred. <i>Note:</i> the sign-in won't complete until this task
     * is complete.
     */
    @Nullable
    public abstract Task<Void> onTransferData(IdpResponse response);
}
