package com.firebase.ui.auth.core;

import android.content.Context;
import com.firebase.client.Firebase;
import com.firebase.ui.R;
import java.lang.reflect.InvocationTargetException;

public enum AuthProviderType {
    GOOGLE  ("google",   "google.GoogleAuthProvider",     R.id.google_button),
    FACEBOOK("facebook", "facebook.FacebookAuthProvider", R.id.facebook_button),
    TWITTER ("twitter",  "twitter.TwitterAuthProvider",   R.id.twitter_button),
    PASSWORD("password", "password.PasswordAuthProvider", R.id.password_section);

    private final static String AUTH_PACKAGE = "com.firebase.ui.auth.";
    private final String mName;
    private final String mProviderName;
    private final int mViewId;

    AuthProviderType(String name, String providerName, int viewId) {
        this.mName = name;
        this.mProviderName = providerName;
        this.mViewId = viewId;
    }

    public String getName() {
        return mName;
    }
    public int getViewId() {
        return mViewId;
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
