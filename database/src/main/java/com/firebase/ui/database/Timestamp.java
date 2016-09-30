package com.firebase.ui.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class Timestamp {
    private long mTimestamp;

    @PropertyName("timestamp")
    private Map<String, String> getServerValue() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public long getTimestamp() {
        return mTimestamp;
    }

    private void setTimestamp(long time) {
        mTimestamp = time;
    }
}
