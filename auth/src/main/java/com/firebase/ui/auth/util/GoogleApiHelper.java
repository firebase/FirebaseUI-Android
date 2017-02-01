package com.firebase.ui.auth.util;

import java.util.ArrayList;
import java.util.List;

public class GoogleApiHelper {
    private static final List<Void> IDS = new ArrayList<>();

    public static int getSafeAutoManageId() {
        IDS.add(null);
        return IDS.size() - 1;
    }
}
