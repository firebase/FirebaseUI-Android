package com.firebase.ui.auth.ui;

import android.os.Bundle;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.R;

@SuppressWarnings("Registered")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppCompatBase extends HelperActivityBase {

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        configureTheme();
    }

    private void configureTheme() {
        setTheme(R.style.FirebaseUI); // Provides default values
        setTheme(getFlowParams().themeId);
    }

}
