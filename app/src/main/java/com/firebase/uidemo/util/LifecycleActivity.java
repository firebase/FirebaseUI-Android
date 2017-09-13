package com.firebase.uidemo.util;

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.support.v7.app.AppCompatActivity;

// TODO remove once arch components are merged into support lib
@SuppressWarnings("Registered")
public class LifecycleActivity extends AppCompatActivity implements LifecycleRegistryOwner {
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mRegistry;
    }
}
