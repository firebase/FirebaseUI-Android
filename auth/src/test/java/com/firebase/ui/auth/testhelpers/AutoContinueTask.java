package com.firebase.ui.auth.testhelpers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/**
 * Subclass of {@link AutoCompleteTask} that also supports continuations.
 */
@SuppressWarnings("unchecked")
public class AutoContinueTask<TResult> extends AutoCompleteTask<TResult> {

    private Object mContinuationResult;

    public AutoContinueTask(TResult result,
                            Object continuationResult,
                            boolean complete,
                            @Nullable Exception exception) {
        super(result, complete, exception);
        mContinuationResult = continuationResult;
    }

    @NonNull
    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWith(
            @NonNull Continuation<TResult, TContinuationResult> continuation) {
        return (Task<TContinuationResult>) Tasks.forResult(mContinuationResult);
    }

    @NonNull
    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(
            @NonNull Continuation<TResult, Task<TContinuationResult>> continuation) {
        return (Task<TContinuationResult>) Tasks.forResult(mContinuationResult);
    }
}
