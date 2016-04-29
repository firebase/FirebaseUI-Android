package com.firebase.ui.auth.api;

import com.google.firebase.FirebaseApp;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(FactoryHeadlessAPI.class)
public class FactoryHeadlessAPIShadow {
    public static HeadlessAPIWrapper headlessAPIWrapper;

    public static void setHeadlessAPIWrapper(HeadlessAPIWrapper newHeadlessAPIWrapper) {
        headlessAPIWrapper = newHeadlessAPIWrapper;
    }

    @Implementation
    public static HeadlessAPIWrapper getHeadlessAPIWrapperInstance(FirebaseApp firebaseApp) {
        return headlessAPIWrapper;
    }

    @Implementation
    public static HeadlessAPIWrapper getHeadlessAPIWrapperInstance(String appName) {
        return headlessAPIWrapper;
    }
}
