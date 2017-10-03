package com.firebase.ui.auth.util;

import android.content.Intent;

public interface ActivityResultHandler {
    /**
     * Handle an activity response.
     *
     * @see android.app.Activity#onActivityResult(int, int, Intent)
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
