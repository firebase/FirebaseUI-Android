package com.firebase.ui.auth.data.client;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.util.Preconditions;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AuthUiInitProvider extends ContentProvider {
    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        Preconditions.checkNotNull(info, "AuthUiInitProvider ProviderInfo cannot be null.");
        if ("com.firebase.ui.auth.authuiinitprovider".equals(info.authority)) {
            throw new IllegalStateException("Incorrect provider authority in manifest. Most" +
                    " likely due to a missing applicationId variable in application's build.gradle.");
        } else {
            super.attachInfo(context, info);
        }
    }

    @Override
    public boolean onCreate() {
        AuthUI.setApplicationContext(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
