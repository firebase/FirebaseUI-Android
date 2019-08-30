package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class OperableViewModel<I, O> extends ViewModelBase<I> {
    private MutableLiveData<O> mOperation = new MutableLiveData<>();

    protected OperableViewModel(Application application) {
        super(application);
    }

    /**
     * Get the observable state of the operation.
     */
    public LiveData<O> getOperation() {
        return mOperation;
    }

    protected void setResult(O output) {
        mOperation.setValue(output);
    }
}
