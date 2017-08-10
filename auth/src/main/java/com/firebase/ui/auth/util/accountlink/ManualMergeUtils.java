package com.firebase.ui.auth.util.accountlink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.Callable;

public final class ManualMergeUtils {
    private ManualMergeUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static <T> Task<T> injectSignInTaskBetweenDataTransfer(
            final HelperActivityBase activity,
            final IdpResponse response,
            final Callable<Task<T>> insertTask) {
        if (response.getUser().getPrevUid() == null
                || activity.getFlowParams().accountLinkingListener == null) try {
            return insertTask.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        final ServiceConnection keepServiceAliveConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {}

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };
        bindService(activity, keepServiceAliveConnection);

        return getLoadDataTask(activity, response)
                .continueWithTask(new Continuation<Void, Task<T>>() {
                    @Override
                    public Task<T> then(@NonNull Task<Void> task) throws Exception {
                        return insertTask.call();
                    }
                })
                .continueWithTask(new Continuation<T, Task<T>>() {
                    @Override
                    public Task<T> then(@NonNull Task<T> task) throws Exception {
                        return getTransferDataTask(activity, response, task);
                    }
                })
                .continueWith(new Continuation<T, T>() {
                    @Override
                    public T then(@NonNull Task<T> task) throws Exception {
                        unbindService(activity, keepServiceAliveConnection);
                        return task.getResult();
                    }
                });
    }

    private static Task<Void> getLoadDataTask(HelperActivityBase activity, IdpResponse response) {
        return getDataTask(activity, new MergeServiceConnection() {
            @Override
            protected Task<Void> getDataTask(ManualMergeService service) {
                return service.onLoadData();
            }
        });
    }

    private static <T> Task<T> getTransferDataTask(HelperActivityBase activity,
                                                   final IdpResponse response,
                                                   final Task<T> originalTask) {
        return getDataTask(activity, new MergeServiceConnection() {
            @Override
            protected Task<Void> getDataTask(ManualMergeService service) {
                return service.onTransferData(response);
            }
        }).continueWith(new Continuation<Void, T>() {
            @Override
            public T then(@NonNull Task<Void> task) throws Exception {
                return originalTask.getResult();
            }
        });
    }

    private static Task<Void> getDataTask(final HelperActivityBase activity,
                                          final MergeServiceConnection connection) {
        TaskCompletionSource<Void> task = new TaskCompletionSource<>();
        bindService(activity, connection.setTask(task));
        return task.getTask().continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) throws Exception {
                unbindService(activity, connection);
                return task.getResult();
            }
        });
    }

    private static void bindService(HelperActivityBase activity,
                                    ServiceConnection connection) {
        Context appContext = activity.getApplicationContext();
        appContext.bindService(
                new Intent(appContext, activity.getFlowParams().accountLinkingListener),
                connection,
                Context.BIND_AUTO_CREATE);
    }

    private static void unbindService(HelperActivityBase activity, ServiceConnection connection) {
        activity.getApplicationContext().unbindService(connection);
    }

    public static final class MergeBinder extends Binder {
        private final ManualMergeService mService;

        public MergeBinder(ManualMergeService service) {
            mService = service;
        }

        public ManualMergeService getService() {
            return mService;
        }
    }

    private abstract static class MergeServiceConnection implements ServiceConnection {
        protected TaskCompletionSource<Void> mTask;

        protected MergeServiceConnection setTask(TaskCompletionSource<Void> task) {
            mTask = task;
            return this;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Task<Void> task = getDataTask(((MergeBinder) service).getService());

            if (task == null) { task = Tasks.forResult(null); }

            task.continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(@NonNull Task<Void> task) throws Exception {
                    mTask.setResult(null);
                    return null;
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTask.trySetException(new IllegalStateException("ManualMergeService disconnected"));
        }

        @Nullable
        protected abstract Task<Void> getDataTask(ManualMergeService service);
    }
}
