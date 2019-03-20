package com.firebase.ui.database.paging.listener;

import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseError;

public interface StateChangedListener {
    void onInitLoading();
    void onLoading();
    void onLoaded();
    void onFinished();
    void onError(@NonNull DatabaseError databaseError);
}
