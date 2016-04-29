package com.firebase.ui.auth.api;


import android.app.PendingIntent;
import android.content.Context;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface HeadlessAPIWrapper {

    public static int HINT_RESULT = 13;

    boolean isAccountExists(String emailAddress);

    List<String> getProviderList(String emailAddress);

    boolean resetEmailPassword(String emailAddress);

    FirebaseUser signInWithEmailPassword(String emailAddress, String password);

    FirebaseUser signInWithCredential(AuthCredential credential);

    FirebaseUser getCurrentUser();

    FirebaseUser createEmailWithPassword(String emailAddress, String password);

    FirebaseUser linkWithCredential(FirebaseUser user, AuthCredential credential) throws ExecutionException;

    boolean isGMSCorePresent(Context context);

    PendingIntent getEmailHints(Context context);

    void setTimeOut(long milliSec);

    void signOut(Context context);
}
