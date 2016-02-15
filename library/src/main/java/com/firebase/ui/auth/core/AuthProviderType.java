package com.firebase.ui.auth.core;

import android.content.Context;
import com.firebase.client.Firebase;
import com.firebase.ui.R;
import java.lang.reflect.InvocationTargetException;

public enum AuthProviderType {
    GOOGLE  ("google",   "google.GoogleAuthProvider",     R.id.google_button, R.id.google_button),
    FACEBOOK("facebook", "facebook.FacebookAuthProvider", R.id.facebook_button, R.id.facebook_button),
    TWITTER ("twitter",  "twitter.TwitterAuthProvider",   R.id.twitter_button, R.id.twitter_button),
    PASSWORD("password", "password.PasswordAuthProvider", R.id.password_section, R.id.password_button);

    private final static String AUTH_PACKAGE = "com.firebase.ui.auth.";
    private final String mName;
    private final String mProviderName;
    private final int mViewId;
    private final int mButtonId;

    AuthProviderType(String name, String providerName, int viewId, int buttonId) {
        this.mName = name;
        this.mProviderName = providerName;
        this.mViewId = viewId;
        this.mButtonId = buttonId;
    }

    public String getName() {
        return mName;
    }
    public int getViewId() {
        return mViewId;
    }

    public int getButtonId() {
        return mButtonId;
    }

    public FirebaseAuthProvider createProvider(Context context, Firebase ref, TokenAuthHandler handler) {
        try {
            Class<? extends FirebaseAuthProvider> clazz = (Class<? extends FirebaseAuthProvider>) Class.forName(AUTH_PACKAGE+mProviderName);
            return clazz.getConstructor(Context.class, AuthProviderType.class, String.class, Firebase.class, TokenAuthHandler.class).newInstance(context, this, this.getName(), ref, handler);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static AuthProviderType getTypeForProvider(FirebaseAuthProvider provider) {
        for (AuthProviderType type : AuthProviderType.values()) {
            if (provider.getProviderName() == type.getName()) {
                return type;
            }
        }
        throw new IllegalArgumentException("The provider you specified is not of a known type");
    }
}
