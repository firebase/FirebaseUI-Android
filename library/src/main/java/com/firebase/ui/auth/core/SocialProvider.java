package com.firebase.ui.auth.core;

import android.content.Context;

import com.firebase.client.Firebase;
import com.firebase.ui.R;
import com.firebase.ui.auth.facebook.FacebookAuthProvider;
import com.firebase.ui.auth.google.GoogleAuthProvider;
import com.firebase.ui.auth.password.PasswordAuthProvider;
import com.firebase.ui.auth.twitter.TwitterAuthProvider;

import java.lang.reflect.InvocationTargetException;

public enum SocialProvider {
    GOOGLE  ("google",   GoogleAuthProvider.class,   R.id.google_button),
    FACEBOOK("facebook", FacebookAuthProvider.class, R.id.facebook_button),
    TWITTER ("twitter",  TwitterAuthProvider.class,  R.id.twitter_button),
    PASSWORD("password", PasswordAuthProvider.class, R.id.password_button);

    private final String mName;
    private final Class<? extends FirebaseAuthProvider> mClass;
    private final int mButtonId;

    SocialProvider(String name, Class<? extends FirebaseAuthProvider> clazz, int button_id) {
        this.mName = name;
        this.mClass = clazz;
        this.mButtonId = button_id;
    }

    public String getName() {
        return mName;
    }
    public int getButtonId() {
        return mButtonId;
    }

    public FirebaseAuthProvider createProvider(Context context, Firebase ref, TokenAuthHandler handler) {
        try {
            return mClass.getConstructor(Context.class, SocialProvider.class, String.class, Firebase.class, TokenAuthHandler.class).newInstance(context, this, this.getName(), ref, handler);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    public static SocialProvider getTypeForProvider(FirebaseAuthProvider provider) {
        for (SocialProvider type : SocialProvider.values()) {
            if (provider.getProviderName() == type.getName()) {
                return type;
            }
        }
        throw new IllegalArgumentException("The provider you specified is not of a known type");
    }
}
