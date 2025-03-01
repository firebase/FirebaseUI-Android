package com.firebase.ui.auth.ui.credentials;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.InvisibleActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.viewmodel.ResourceObserver;

import androidx.credentials.CreateCredentialCustomizationOption;
import androidx.credentials.CredentialOption;
import androidx.credentials.CustomCredentialOptions;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.PublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredentialOption.Builder;
import androidx.credentials.PublicKeyCredentialOption.Companion;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialsException;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.CredentialException;
import androidx.credentials.exceptions.CreateCredentialCustomizationException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.BeginCreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialCustomizationException.CredentialCustomizationException;
import androidx.credentials.exceptions.CreateCredentialException.CreateCredentialException;
import androidx.credentials.exceptions.BeginCreateCredentialException.BeginCreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialCustomizationException.CredentialCustomizationException;
import androidx.credentials.exceptions.CreateCredentialException.CreateCredentialException;
import androidx.credentials.exceptions.BeginCreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.BeginCreateCredentialException.BeginCreateCredentialException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.exceptions.BeginCreateCredentialException;
import androidx.lifecycle.ViewModelProvider;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.SaveCredentialRequest;
import androidx.credentials.PasswordCredential;
import androidx.credentials.CredentialManager;
import androidx.credentials.SaveCredentialRequest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.CreateCredentialCustomizationOption;
import androidx.credentials.CreateCredentialCustomizationOption.CredentialCustomizationException;
import androidx.credentials.CredentialOption;
import androidx.credentials.CustomCredentialOptions;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.PublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredentialOption.Builder;
import androidx.credentials.PublicKeyCredentialOption.Companion;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialsException;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.CredentialException;
import androidx.credentials.exceptions.CreateCredentialCustomizationException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.BeginCreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialCustomizationException.CredentialCustomizationException;
import androidx.credentials.exceptions.CreateCredentialException.CreateCredentialException;
import androidx.credentials.exceptions.BeginCreateCredentialException.BeginCreateCredentialException;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.SaveCredentialRequest;
import androidx.credentials.PasswordCredential;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.firebase.ui.auth.ErrorCodes;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Invisible Activity used for saving credentials to SmartLock.
 */
public class CredentialSaveActivity extends InvisibleActivityBase {
    private static final String TAG = "CredentialSaveActivity";
    private CredentialManager credentialManager;
    private SmartLockHandler mHandler;

    @NonNull
    public static Intent createIntent(Context context,
                                      FlowParameters flowParams,
                                      PasswordCredential passwordCredential,
                                      IdpResponse response) {
        return createBaseIntent(context, CredentialSaveActivity.class, flowParams)
                .putExtra(ExtraConstants.CREDENTIAL, passwordCredential)
                .putExtra(ExtraConstants.IDP_RESPONSE, response);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final IdpResponse response = getIntent().getParcelableExtra(ExtraConstants.IDP_RESPONSE);
        PasswordCredential passwordCredential = getIntent().getParcelableExtra(ExtraConstants.CREDENTIAL);
        credentialManager = CredentialManager.create(this);
        
        try {
                credentialManager.saveCredential(new SaveCredentialRequest.Builder()
                    .setPasswordCredential(passwordCredential)
                    .build());
            } catch (NoCredentialsException e) {
                Log.w(TAG, "Error no credential",e);
                setResult(Resource.forFailure(e));
            } catch (Exception e) {
                Log.w(TAG, "Error no credential",e);
                 setResult(Resource.forFailure(e));
            } catch(BeginCreateCredentialException e){
                Log.w(TAG, "Error no credential",e);
                setResult(Resource.forFailure(e));
            } catch(CreateCredentialException e){
                Log.w(TAG, "Error no credential",e);
                setResult(Resource.forFailure(e));
            }
            
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
