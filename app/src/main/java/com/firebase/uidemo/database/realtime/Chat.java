package com.firebase.uidemo.database.realtime;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.uidemo.database.AbstractChat;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Chat extends AbstractChat {
    private String mName;
    private String mMessage;
    private String mUid;

    public Chat() {
        // Needed for Firebase
    }

    public Chat(@Nullable String name, @Nullable String message, @NonNull String uid) {
        mName = name;
        mMessage = message;
        mUid = uid;
    }

    @Override
    @Nullable
    public String getName() {
        return mName;
    }

    public void setName(@Nullable String name) {
        mName = name;
    }

    @Override
    @Nullable
    public String getMessage() {
        return mMessage;
    }

    public void setMessage(@Nullable String message) {
        mMessage = message;
    }

    @Override
    @NonNull
    public String getUid() {
        return mUid;
    }

    public void setUid(@NonNull String uid) {
        mUid = uid;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Chat chat = (Chat) o;

        return mUid.equals(chat.mUid)
                && (mName == null ? chat.mName == null : mName.equals(chat.mName))
                && (mMessage == null ? chat.mMessage == null : mMessage.equals(chat.mMessage));
    }

    @Override
    public int hashCode() {
        int result = mName == null ? 0 : mName.hashCode();
        result = 31 * result + (mMessage == null ? 0 : mMessage.hashCode());
        result = 31 * result + mUid.hashCode();
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "Chat{" +
                "mName='" + mName + '\'' +
                ", mMessage='" + mMessage + '\'' +
                ", mUid='" + mUid + '\'' +
                '}';
    }
}
