package com.firebase.ui.auth.util;

import java.util.concurrent.atomic.AtomicInteger;

public class GoogleApiHelper {
    private static final AtomicInteger SAFE_ID = new AtomicInteger(0);

    public static int getSafeAutoManageId() {
        return SAFE_ID.getAndIncrement();
    }
}
