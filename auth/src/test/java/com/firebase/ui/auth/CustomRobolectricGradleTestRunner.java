package com.firebase.ui.auth;

import com.firebase.ui.auth.api.FactoryHeadlessAPI;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;

public class CustomRobolectricGradleTestRunner extends RobolectricGradleTestRunner {
    public CustomRobolectricGradleTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public InstrumentationConfiguration createClassLoaderConfig() {
        InstrumentationConfiguration.Builder builder = InstrumentationConfiguration.newBuilder();
        builder.addInstrumentedClass(FactoryHeadlessAPI.class.getName());
        return builder.build();
    }

}
